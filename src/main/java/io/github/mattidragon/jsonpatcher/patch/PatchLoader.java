package io.github.mattidragon.jsonpatcher.patch;

import com.google.common.base.Suppliers;
import dev.mattidragon.jsonpatcher.lang.error.Diagnostic;
import dev.mattidragon.jsonpatcher.lang.error.DiagnosticsBuilder;
import dev.mattidragon.jsonpatcher.lang.parse.Lexer;
import dev.mattidragon.jsonpatcher.lang.parse.Parser;
import dev.mattidragon.jsonpatcher.lang.parse.metadata.MetadataNull;
import dev.mattidragon.jsonpatcher.lang.runtime.bytecode.CompilationException;
import dev.mattidragon.jsonpatcher.lang.runtime.bytecode.compiler.CompilerOptions;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.EvaluationEnvironment;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.Library;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.LibraryGroup;
import dev.mattidragon.jsonpatcher.lang.runtime_shared.Value;
import io.github.mattidragon.jsonpatcher.JsonPatcher;
import io.github.mattidragon.jsonpatcher.config.Config;
import io.github.mattidragon.jsonpatcher.misc.MetadataOps;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PatchLoader {
    private static final ResourceFinder finder = new ResourceFinder("jsonpatch", ".jsonpatch");

    public static PatchStorage load(Executor executor, ResourceManager manager) {
        var files = finder.findResources(manager);
        var futures = new ArrayList<CompletableFuture<Void>>();
        var patches = Collections.synchronizedList(new ArrayList<Patch>());
        var environment = new EvaluationEnvironment(CompilerOptions.DEFAULT); // TODO: offer config
        environment.bootstrap();

        var errorCount = new AtomicInteger(0);
        var warnCount = new AtomicInteger(0);
        for (var entry : files.entrySet()) {
            futures.add(CompletableFuture.runAsync(() -> {
                var patch = loadPatch(entry, environment, errorCount, warnCount);
                if (patch != null) {
                    patches.add(patch);
                }
            }, executor));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        if (errorCount.get() > 0) {
            var message = "Failed to load %s patch(es). See logs/jsonpatch.log for details".formatted(errorCount.get());
            ErrorLogger.CURRENT.get().accept(Text.literal(message).formatted(Formatting.RED));
            JsonPatcher.MAIN_LOGGER.error(message);
            if (Config.MANAGER.get().throwOnFailure()) {
                throw new IllegalStateException(message);
            }
        }
        if (warnCount.get() > 0) {
            var message = "Encountered warnings while loading %s patch(es). See logs/jsonpatch.log for details".formatted(warnCount.get());
            ErrorLogger.CURRENT.get().accept(Text.literal(message).formatted(Formatting.YELLOW));
            JsonPatcher.MAIN_LOGGER.warn(message);
        }
        return new PatchStorage(patches);
    }

    @Nullable
    private static Patch loadPatch(Map.Entry<Identifier, Resource> entry, EvaluationEnvironment environment, AtomicInteger errorCount, AtomicInteger warnCount) {
        var id = finder.toResourceId(entry.getKey());
        var resource = entry.getValue();

        try {
            var code = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            var diagnosticsBuilder = new DiagnosticsBuilder();

            var lexResult = Lexer.lex(code, id.toString(), diagnosticsBuilder);
            var parseResult = Parser.parse(lexResult.tokens(), diagnosticsBuilder);

            var diagnostics = diagnosticsBuilder.build();
            var errors = diagnostics.errors();
            var warnings = diagnostics.warnings();

            if (!errors.isEmpty()) {
                JsonPatcher.RELOAD_LOGGER.warn("Failed to load patch {} from {}:\n{}", id, entry.getKey(), errors
                        .stream()
                        .map(Diagnostic::toDisplay)
                        .collect(Collectors.joining("\n")));
                errorCount.incrementAndGet();
            }
            if (!warnings.isEmpty()) {
                JsonPatcher.RELOAD_LOGGER.warn("Warnings while loading patch {} from {}:\n{}", id, entry.getKey(), warnings
                        .stream()
                        .map(Diagnostic::toDisplay)
                        .collect(Collectors.joining("\n")));
                warnCount.addAndGet(warnings.size());
            }

            if (errors.isEmpty()) {
                return validateAndBuild(id, parseResult, environment);
            }
        } catch (IOException | CompilationException | IllegalStateException e) {
            JsonPatcher.RELOAD_LOGGER.error("Failed to load patch {} from {}", id, entry.getKey(), e);
            errorCount.incrementAndGet();
        } catch (RuntimeException e) {
            JsonPatcher.RELOAD_LOGGER.error("Unexpected error while loading patches", e);
            errorCount.incrementAndGet();
        }
        return null;
    }

    @Nullable
    private static Patch validateAndBuild(Identifier id, Parser.Result result, EvaluationEnvironment environment) {
        var meta = result.metadata();
        if (meta.has("enabled") && !meta.getBoolean("enabled")) {
            return null;
        }

        if (!JsonPatcher.isSupportedVersion(meta.getString("version"))) {
            throw new IllegalStateException("Unsupported patch version '%s'".formatted(meta.getString("version")));
        }

        List<PatchTarget> target;
        if (meta.has("target")) {
            target = PatchTarget.LIST_CODEC.parse(MetadataOps.INSTANCE, meta.get("target"))
                    .getOrThrow(error -> new IllegalStateException("Failed to parse target: %s".formatted(error)));
        } else {
            target = List.of();
        }

        double priority;
        if (meta.has("priority")) {
            priority = meta.getNumber("priority");
        } else {
            priority = 0;
        }

        @Nullable LibraryMetadata libraryMetadata = null;
        if (meta.has("library")) {
            var data = meta.get("library");
            if (data == MetadataNull.INSTANCE) {
                libraryMetadata = LibraryMetadata.DEFAULT;
            } else {
                libraryMetadata = LibraryMetadata.CODEC.parse(MetadataOps.INSTANCE, data)
                        .getOrThrow(error -> new IllegalStateException("Failed to parse library metadata: %s".formatted(error)));
            }
        }

        var className = "jsonpatch/"
                        + id.getNamespace().replace("-|\\.", "_")
                        + id.getPath().replace("-|\\.", "_");
        // TODO: allow reflection when patches can be trusted
        var added = environment.addProgram(result.program(), result.treeMetadata(), id.toString(), className, Set.of(LibraryGroup.DEFAULT));

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

        return new Patch(added, id, target, priority, meta.has("metapatch"));
    }
}
