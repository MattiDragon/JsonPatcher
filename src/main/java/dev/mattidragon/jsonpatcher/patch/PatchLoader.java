package dev.mattidragon.jsonpatcher.patch;

import com.google.common.base.Suppliers;
import dev.mattidragon.jsonpatcher.JsonPatcher;
import dev.mattidragon.jsonpatcher.config.Config;
import dev.mattidragon.jsonpatcher.lang.ast.meta.MetadataKey;
import dev.mattidragon.jsonpatcher.lang.ast.meta.TreeMetadata;
import dev.mattidragon.jsonpatcher.lang.error.Diagnostic;
import dev.mattidragon.jsonpatcher.lang.error.DiagnosticsBuilder;
import dev.mattidragon.jsonpatcher.lang.parse.Lexer;
import dev.mattidragon.jsonpatcher.lang.parse.Parser;
import dev.mattidragon.jsonpatcher.lang.parse.metadata.MetadataNull;
import dev.mattidragon.jsonpatcher.lang.parse.metadata.MetadataString;
import dev.mattidragon.jsonpatcher.lang.parse.metadata.PatchMetadata;
import dev.mattidragon.jsonpatcher.lang.runtime.bytecode.CompilationException;
import dev.mattidragon.jsonpatcher.lang.runtime.bytecode.compiler.CompilerOptions;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.EvaluationEnvironment;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.Library;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.LibraryGroup;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.ProgramData;
import dev.mattidragon.jsonpatcher.lang.runtime.lib.builder.LibraryBuilder;
import dev.mattidragon.jsonpatcher.lang.runtime.value.Value;
import dev.mattidragon.jsonpatcher.metapatch.MetapatchLibrary;
import dev.mattidragon.jsonpatcher.misc.DumpManager;
import dev.mattidragon.jsonpatcher.misc.MetadataOps;
import dev.mattidragon.jsonpatcher.misc.ModLibraryGroups;
import dev.mattidragon.jsonpatcher.patch.global.GlobalPatchLoader;
import dev.mattidragon.jsonpatcher.patch.global.GlobalPatchScanner;
import dev.mattidragon.jsonpatcher.trust.TrustChecker;
import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PatchLoader {
    private static final ResourceFinder FINDER = new ResourceFinder("jsonpatch", ".jsonpatch");

    public static PatchStorage loadPatches(Executor executor, ResourceManager manager, ResourceType resourceType) {
        var files = FINDER.findResources(manager);
        var futures = new ArrayList<CompletableFuture<Void>>();
        var patches = Collections.synchronizedList(new ArrayList<Patch>());

        var environment = new EvaluationEnvironment(CompilerOptions.DEFAULT); // TODO: offer config
        if (Config.MANAGER.get().dumpCompiledPatches()) {
            environment.enableDumping(DumpManager.getDumpPath("classes/" + resourceType.getDirectory()));
        }
        environment.enableLogging(value -> JsonPatcher.RELOAD_LOGGER.info("Debug message from patch: {}", value));
        environment.bootstrap();
        var metapatchLibrary = new MetapatchLibrary(manager);
        environment.addLibrary(new Library(
                ModLibraryGroups.METAPATCH,
                "metapatch",
                Suppliers.memoize(() -> new LibraryBuilder(MetapatchLibrary.class, metapatchLibrary).build())
        ));
        GlobalPatchLoader.getGlobalLibs().forEach(environment::addLibrary);

        var errorCount = new AtomicInteger(0);
        var warnCount = new AtomicInteger(0);
        for (var entry : files.entrySet()) {
            var trust = TrustChecker.getTrust(entry.getValue().getPack());
            futures.add(CompletableFuture.runAsync(() -> {
                var patch = loadPatch(FINDER.toResourceId(entry.getKey()), entry.getKey(), entry.getValue()::getInputStream, environment, errorCount, warnCount, trust);
                if (patch != null) {
                    patches.add(patch);
                }
            }, executor));
        }
        for (var entry : GlobalPatchScanner.scan(resourceType).entrySet()) {
            futures.add(CompletableFuture.runAsync(() -> {
                var patch = loadPatch(entry.getKey().withPath(path -> "/" + path.substring(path.indexOf('/', 1) + 1)), entry.getKey(), entry.getValue(), environment, errorCount, warnCount, TrustLevel.MODPACK);
                if (patch != null) {
                    patches.add(patch);
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        if (errorCount.get() > 0) {
            var message = "Failed to load %s patch(es). See jsonpatcher/jsonpatcher.log for details".formatted(errorCount.get());
            ErrorLogger.CURRENT.get().accept(Text.literal(message).formatted(Formatting.RED));
            JsonPatcher.MAIN_LOGGER.error(message);
            if (Config.MANAGER.get().throwOnFailure()) {
                throw new IllegalStateException(message);
            }
        }
        if (warnCount.get() > 0) {
            var message = "Encountered warnings while loading %s patch(es). See jsonpatcher/jsonpatcher.log for details".formatted(warnCount.get());
            ErrorLogger.CURRENT.get().accept(Text.literal(message).formatted(Formatting.YELLOW));
            JsonPatcher.MAIN_LOGGER.warn(message);
        }
        return new PatchStorage(patches, metapatchLibrary);
    }

    @Nullable
    private static Patch loadPatch(Identifier id, Identifier location, InputSupplier<InputStream> streamSupplier, EvaluationEnvironment environment, AtomicInteger errorCount, AtomicInteger warnCount, TrustLevel trust) {
        try {
            var code = new String(streamSupplier.get().readAllBytes(), StandardCharsets.UTF_8);
            var diagnosticsBuilder = new DiagnosticsBuilder();

            var lexResult = Lexer.lex(code, id.toString(), diagnosticsBuilder);
            var parseResult = Parser.parse(lexResult.tokens(), diagnosticsBuilder);
            
            var built = validateAndBuild(id, parseResult, environment, diagnosticsBuilder, trust);

            var diagnostics = diagnosticsBuilder.build();
            var errors = diagnostics.errors();
            var warnings = diagnostics.warnings();

            if (!errors.isEmpty()) {
                JsonPatcher.RELOAD_LOGGER.error("Failed to load patch {} from {}:\n{}", id, location, errors
                        .stream()
                        .map(Diagnostic::toDisplay)
                        .collect(Collectors.joining("\n")));
                errorCount.incrementAndGet();
            }
            if (!warnings.isEmpty()) {
                JsonPatcher.RELOAD_LOGGER.warn("Warnings while loading patch {} from {}:\n{}", id, location, warnings
                        .stream()
                        .map(Diagnostic::toDisplay)
                        .collect(Collectors.joining("\n")));
                warnCount.addAndGet(warnings.size());
            }

            if (errors.isEmpty()) {
                return built;
            }
        } catch (IOException | CompilationException | IllegalStateException e) {
            JsonPatcher.RELOAD_LOGGER.error("Failed to load patch {} from {}", id, location, e);
            errorCount.incrementAndGet();
        } catch (RuntimeException e) {
            JsonPatcher.RELOAD_LOGGER.error("Unexpected error while loading patches", e);
            errorCount.incrementAndGet();
        }
        return null;
    }

    @Nullable
    private static Patch validateAndBuild(Identifier id, Parser.Result result, EvaluationEnvironment environment, DiagnosticsBuilder diagnosticsBuilder, TrustLevel trust) {
        var roles = new HashSet<String>();

        var treeMeta = result.treeMetadata();
        var meta = result.metadata();
        if (meta.has("enabled") && !meta.getBoolean("enabled")) {
            return null;
        }

        if (!validateVersion(diagnosticsBuilder, meta, treeMeta)) return null;
        var target = getTargets(diagnosticsBuilder, meta, treeMeta, roles);
        var priority = getPriority(meta);
        var libraryMetadata = getLibraryMetadata(diagnosticsBuilder, meta, treeMeta, roles);
        var isMetapatch = isIsMetapatch(diagnosticsBuilder, meta, treeMeta, roles);

        if (roles.size() > 1) {
            diagnosticsBuilder.addDiagnostic(new PatchLoaderDiagnostic(
                    null,
                    "A single patch may only have one role. %s has %s: %s"
                            .formatted(id, roles.size(), String.join(", ", roles)),
                    Diagnostic.Kind.ERROR,
                    5
            ));
        }

        var className = "jsonpatcher_patches/"
                        + id.getNamespace().replaceAll("[-.]", "_")
                        + "/"
                        + id.getPath().replaceAll("[-.]", "_");

        var builder = ProgramData.builder(result)
                .scriptName(id.toString())
                .className(className);

        if (isMetapatch) builder.allowLibraryGroup(ModLibraryGroups.METAPATCH);
        if (trust.ordinal() >= Config.MANAGER.get().reflectionMinTrustLevel().ordinal()) {
            builder.allowLibraryGroup(LibraryGroup.REFLECTION);
        }

        // TODO: compilation exception will cause diagnostics to be ignored, fix
        var added = environment.addProgram(builder.build());

        if (libraryMetadata != null) {
            Supplier<Value.ObjectValue> supplier = () -> {
                var obj = new Value.ObjectValue();
                added.run(obj);
                return obj;
            };
            if (libraryMetadata.shared()) {
                supplier = Suppliers.memoize(supplier::get);
            }
            var library = new Library(LibraryGroup.DEFAULT, id.toString(), supplier);
            environment.addLibrary(library);
        }

        return new Patch(added, id, target, priority, isMetapatch, trust);
    }

    private static boolean isIsMetapatch(DiagnosticsBuilder diagnosticsBuilder, PatchMetadata meta, TreeMetadata treeMeta, HashSet<String> roles) {
        var isMetapatch = meta.has("metapatch");
        if (isMetapatch) {
            if (!(meta.get("metapatch") instanceof MetadataNull)) {
                diagnosticsBuilder.addDiagnostic(new PatchLoaderDiagnostic(
                        treeMeta.get(meta.get("metapatch"), MetadataKey.MAIN_POS).orElse(null),
                        "Metapatch metadata should be empty",
                        Diagnostic.Kind.ERROR,
                        4
                ));
            }
            roles.add("metapatch");
        }
        return isMetapatch;
    }

    public static @Nullable LibraryMetadata getLibraryMetadata(DiagnosticsBuilder diagnosticsBuilder, PatchMetadata meta, TreeMetadata treeMeta, HashSet<String> roles) {
        if (meta.has("library")) {
            var data = meta.get("library");
            if (data instanceof MetadataNull) {
                return LibraryMetadata.DEFAULT;
            } else {
                var dataResult = LibraryMetadata.CODEC.parse(MetadataOps.INSTANCE, data);
                if (dataResult.isSuccess()) {
                    return dataResult.getOrThrow();
                } else {
                    diagnosticsBuilder.addDiagnostic(new PatchLoaderDiagnostic(
                            treeMeta.get(data, MetadataKey.MAIN_POS).orElse(null),
                            "Failed to parse library metadata: %s".formatted(dataResult.error().orElseThrow().message()),
                            Diagnostic.Kind.ERROR,
                            3
                    ));
                }
            }
            roles.add("library");
        }
        return null;
    }

    public static boolean validateVersion(DiagnosticsBuilder diagnosticsBuilder, PatchMetadata meta, TreeMetadata treeMeta) {
        if (!meta.has("version") || !(meta.get("version") instanceof MetadataString(var version))) {
            var pos = meta.has("version")
                    ? treeMeta.get(meta.get("version"), MetadataKey.MAIN_POS).orElse(null)
                    : null;
            diagnosticsBuilder.addDiagnostic(new PatchLoaderDiagnostic(
                    pos,
                    "Unsupported patch version '%s'".formatted(meta.getString("version")),
                    Diagnostic.Kind.ERROR,
                    0
            ));
            return false;
        }
        if (!JsonPatcher.isSupportedVersion(version)) {
            diagnosticsBuilder.addDiagnostic(new PatchLoaderDiagnostic(
                    treeMeta.get(meta.get("version"), MetadataKey.MAIN_POS).orElse(null),
                    "Unsupported patch version '%s'".formatted(meta.getString("version")),
                    Diagnostic.Kind.ERROR,
                    1
            ));
            return false;
        }
        return true;
    }

    private static List<PatchTarget> getTargets(DiagnosticsBuilder diagnosticsBuilder, PatchMetadata meta, TreeMetadata treeMeta, HashSet<String> roles) {
        List<PatchTarget> target;
        if (meta.has("target")) {
            var dataResult = PatchTarget.LIST_CODEC.parse(MetadataOps.INSTANCE, meta.get("target"));
            if (dataResult.isSuccess()) {
                target = dataResult.getOrThrow();
            } else {
                target = List.of();
                diagnosticsBuilder.addDiagnostic(new PatchLoaderDiagnostic(
                        treeMeta.get(meta.get("target"), MetadataKey.MAIN_POS).orElse(null),
                        "Failed to parse target: %s".formatted(dataResult.error().orElseThrow().message()),
                        Diagnostic.Kind.ERROR,
                        2
                ));
            }

            roles.add("patch");
        } else {
            target = List.of();
        }
        return target;
    }

    public static double getPriority(PatchMetadata meta) {
        double priority;
        if (meta.has("priority")) {
            priority = meta.getNumber("priority");
        } else {
            priority = 0;
        }
        return priority;
    }
}
