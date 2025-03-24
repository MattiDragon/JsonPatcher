package dev.mattidragon.jsonpatcher.patch.global;

import com.google.common.base.Suppliers;
import dev.mattidragon.jsonpatcher.JsonPatcher;
import dev.mattidragon.jsonpatcher.config.Config;
import dev.mattidragon.jsonpatcher.lang.ast.meta.MetadataKey;
import dev.mattidragon.jsonpatcher.lang.ast.meta.TreeMetadata;
import dev.mattidragon.jsonpatcher.lang.error.Diagnostic;
import dev.mattidragon.jsonpatcher.lang.error.DiagnosticsBuilder;
import dev.mattidragon.jsonpatcher.lang.parse.Lexer;
import dev.mattidragon.jsonpatcher.lang.parse.Parser;
import dev.mattidragon.jsonpatcher.lang.parse.metadata.MetadataString;
import dev.mattidragon.jsonpatcher.lang.parse.metadata.PatchMetadata;
import dev.mattidragon.jsonpatcher.lang.runtime.bytecode.CompilationException;
import dev.mattidragon.jsonpatcher.lang.runtime.bytecode.compiler.CompilerOptions;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.EvaluationEnvironment;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.Library;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.LibraryGroup;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.ProgramData;
import dev.mattidragon.jsonpatcher.lang.runtime.value.Value;
import dev.mattidragon.jsonpatcher.patch.PatchLoader;
import dev.mattidragon.jsonpatcher.patch.PatchLoaderDiagnostic;
import dev.mattidragon.jsonpatcher.patch.Patcher;
import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GlobalPatchLoader {
    private static List<Library> globalLibs = new ArrayList<>();
    private static List<GlobalPatch> globalPatches = new ArrayList<>();
    private static final AtomicInteger errorCount = new AtomicInteger();
    private static final AtomicInteger warnCount = new AtomicInteger();

    private static List<GlobalPatchSource> findSources() {
        var sources = new ArrayList<GlobalPatchSource>();
        sources.add(new GlobalPatchSource( "scripts:global", JsonPatcher.DATA_DIR.resolve("scripts"), TrustLevel.MODPACK));
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            mod.findPath("jsonpatcher/scripts")
                    .ifPresent(path ->
                            sources.add(new GlobalPatchSource("mod:" + mod.getMetadata().getId(), path, TrustLevel.MOD)));
        }
        return sources;
    }

    public static synchronized void loadGlobalPatches() {
        // Allocating new lists keeps previous ones valid. This prevents threading issues
        globalLibs = new ArrayList<>();
        globalPatches = new ArrayList<>();
        warnCount.set(0);
        errorCount.set(0);

        var environment = new EvaluationEnvironment(CompilerOptions.DEFAULT);
        if (Config.MANAGER.get().dumpCompiledPatches()) {
            environment.enableDumping(JsonPatcher.DATA_DIR.resolve("dump").resolve("global"));
        }
        environment.enableLogging(v -> JsonPatcher.RELOAD_LOGGER.debug("Debug from global patch: {}", v));
        environment.bootstrap();

        for (var source : findSources()) {
            globalPatches.addAll(loadPatchDir(source, environment));
        }

        JsonPatcher.RELOAD_LOGGER.info("Loaded {} global patches. ({} libraries)", globalPatches.size(), getGlobalLibs().size());
        if (warnCount.get() > 0) {
            JsonPatcher.RELOAD_LOGGER.warn("Encountered {} warnings while loading global patches. See jsonpatcher/jsonpatcher.log for details.", warnCount.get());
        }
        if (errorCount.get() > 0) {
            JsonPatcher.RELOAD_LOGGER.error("Encountered {} errors while loading global patches. See jsonpatcher/jsonpatcher.log for details.", errorCount.get());
        }
    }

    // Synchronized to block access while reloading
    public static synchronized List<Library> getGlobalLibs() {
        return Collections.unmodifiableList(globalLibs);
    }

    // Synchronized to block access while reloading
    public static void runEntrypoint(GlobalPatch.Entrypoint entrypoint) {
        List<GlobalPatch> toRun;
        synchronized (GlobalPatchLoader.class) {
            toRun = globalPatches.stream()
                    .filter(globalPatch -> globalPatch.entrypoint() == entrypoint)
                    .toList();
        }

        var errors = new ArrayList<RuntimeException>();

        try (var executor = Executors.newSingleThreadExecutor()) {
            for (var patch : toRun) {
                Patcher.runPatch(patch, executor, errors::add, new Value.ObjectValue());
            }
        }

        if (!errors.isEmpty()) {
            errors.forEach(error -> JsonPatcher.RELOAD_LOGGER.error("Error while running entrypoint patches for {} entrypoint", entrypoint, error));
            JsonPatcher.MAIN_LOGGER.error("Encountered {} errors while running entrypoint patches for {}", errors.size(), entrypoint);
        }
    }

    private static List<GlobalPatch> loadPatchDir(GlobalPatchSource source, EvaluationEnvironment environment) {
        var patches = new ArrayList<GlobalPatch>();
        try (var stream = Files.walk(source.path())) {
            var files = stream.filter(Files::isRegularFile).toList();
            for (var file : files) {
                if (!file.toString().endsWith(".jsonpatch")) {
                    continue;
                }

                var fileName = source.path().relativize(file).toString();
                var id = source.idPrefix() + ":" + fileName
                        .substring(0, fileName.length() - ".jsonpatch".length())
                        .replace(file.getFileSystem().getSeparator(), "/")
                        .replaceFirst("^\\./", "");
                var code = Files.readString(file);
                var patch = loadPatch(id, code, source.trustLevel(), environment);
                if (patch == null) continue;
                patches.add(patch);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load global patches with prefix " + source.idPrefix(), e);
        }
        return patches;
    }

    private static @Nullable GlobalPatch loadPatch(String id, String code, TrustLevel trust, EvaluationEnvironment environment) {
        var diagnosticsBuilder = new DiagnosticsBuilder();

        var lexResult = Lexer.lex(code, id, diagnosticsBuilder);
        var parseResult = Parser.parse(lexResult.tokens(), diagnosticsBuilder);

        var treeMeta = parseResult.treeMetadata();
        var meta = parseResult.metadata();
        if (meta.has("enabled") && !meta.getBoolean("enabled")) {
            return null;
        }

        var roles = new HashSet<String>();

        var priority = PatchLoader.getPriority(meta);
        var libraryMetadata = PatchLoader.getLibraryMetadata(diagnosticsBuilder, meta, treeMeta, roles);
        var entrypoint = getEntrypointMeta(meta, treeMeta, diagnosticsBuilder, roles);

        var className = "jsonpatcher_global/"
                        + id.replaceAll("[-.:]", "_");

        var builder = ProgramData.builder(parseResult)
                .scriptName(id)
                .className(className);

        if (trust.ordinal() >= Config.MANAGER.get().reflectionMinTrustLevel().ordinal()) {
            builder.allowLibraryGroup(LibraryGroup.REFLECTION);
        }

        EvaluationEnvironment.AddedProgram added;
        try {
            added = environment.addProgram(builder.build());
        } catch (CompilationException e) {
            diagnosticsBuilder.addDiagnostic(new PatchLoaderDiagnostic(
                    null,
                    e.getMessage(),
                    Diagnostic.Kind.ERROR,
                    20
            ));
            added = null;
        }

        var diagnostics = diagnosticsBuilder.build();
        var errors = diagnostics.errors();
        if (!errors.isEmpty()) {
            JsonPatcher.RELOAD_LOGGER.error("Errors while loading global patch {}:\n{}", id, errors.stream().map(Diagnostic::toDisplay).collect(Collectors.joining("\n")));
            errorCount.incrementAndGet();
            return null;
        }
        var warnings = diagnostics.warnings();
        if (!warnings.isEmpty()) {
            JsonPatcher.RELOAD_LOGGER.warn("Warnings while loading global patch {}:\n{}", id, warnings.stream().map(Diagnostic::toDisplay).collect(Collectors.joining("\n")));
            warnCount.incrementAndGet();
        }

        if (added == null) {
            return null;
        }

        if (libraryMetadata != null) {
            var finalAdded = added;
            Supplier<Value.ObjectValue> supplier = () -> {
                var obj = new Value.ObjectValue();
                finalAdded.run(obj);
                return obj;
            };
            if (libraryMetadata.shared()) {
                supplier = Suppliers.memoize(supplier::get);
            }
            var lib = new Library(
                    LibraryGroup.DEFAULT,
                    id,
                    supplier
            );
            globalLibs.add(lib);
            environment.addLibrary(lib);
        }

        return new GlobalPatch(added, id, priority, trust, entrypoint);
    }

    private static @Nullable GlobalPatch.Entrypoint getEntrypointMeta(PatchMetadata meta, TreeMetadata treeMeta, DiagnosticsBuilder diagnosticsBuilder, Set<String> roles) {
        if (!meta.has("init")) return null;

        roles.add("init");

        if (!(meta.get("init") instanceof MetadataString(var string))) {
            diagnosticsBuilder.addDiagnostic(new PatchLoaderDiagnostic(
                    treeMeta.get(meta.get("init"), MetadataKey.MAIN_POS).orElse(null),
                    "@init must be set to string",
                    Diagnostic.Kind.ERROR,
                    21
            ));
            return null;
        }

        return switch (string) {
            case "main" -> GlobalPatch.Entrypoint.MAIN;
            case "client" -> GlobalPatch.Entrypoint.CLIENT;
            default -> {
                diagnosticsBuilder.addDiagnostic(new PatchLoaderDiagnostic(
                        treeMeta.get(meta.get("init"), MetadataKey.MAIN_POS).orElse(null),
                        "Invalid entrypoint: " + string,
                        Diagnostic.Kind.ERROR,
                        22
                ));
                yield null;
            }
        };
    }
}
