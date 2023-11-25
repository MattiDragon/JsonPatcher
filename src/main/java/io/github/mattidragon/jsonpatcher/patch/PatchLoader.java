package io.github.mattidragon.jsonpatcher.patch;

import io.github.mattidragon.jsonpatcher.JsonPatcher;
import io.github.mattidragon.jsonpatcher.misc.ValueOps;
import io.github.mattidragon.jsonpatcher.config.Config;
import io.github.mattidragon.jsonpatcher.lang.parse.Lexer;
import io.github.mattidragon.jsonpatcher.lang.parse.Parser;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PatchLoader {
    private static final ResourceFinder finder = new ResourceFinder("jsonpatch", ".jsonpatch");

    public static PatchStorage load(Executor executor, ResourceManager manager) {
        var files = finder.findResources(manager);
        var futures = new ArrayList<CompletableFuture<Void>>();
        var patches = Collections.synchronizedList(new ArrayList<Patch>());
        var errorCount = new AtomicInteger(0);
        for (var entry : files.entrySet()) {
            futures.add(CompletableFuture.runAsync(() -> {
                var patch = loadPatch(entry, errorCount);
                if (patch != null) {
                    patches.add(patch);
                }
            }, executor));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        if (errorCount.get() > 0) {
            JsonPatcher.MAIN_LOGGER.error("Failed to load {} patch(es). See logs/jsonpatch.log for details", errorCount.get());
            if (Config.MANAGER.get().abortOnFailure()) {
                throw new IllegalStateException("Failed to load %s patch(es). See logs/jsonpatch.log for details".formatted(errorCount.get()));
            }
        }
        return new PatchStorage(patches);
    }

    @Nullable
    private static Patch loadPatch(Map.Entry<Identifier, Resource> entry, AtomicInteger errorCount) {
        var id = finder.toResourceId(entry.getKey());
        var resource = entry.getValue();

        try {
            var code = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            var lexResult = Lexer.lex(code, id.toString());

            var parseResult = Parser.parse(lexResult.tokens());

            if (parseResult instanceof Parser.Result.Fail fail) {
                logParseError(entry, fail, id);
                errorCount.incrementAndGet();
            } else {
                return validateAndBuild(id, (Parser.Result.Success) parseResult);
            }
        } catch (IOException | Lexer.LexException | IllegalStateException e) {
            JsonPatcher.RELOAD_LOGGER.error("Failed to load patch {} from {}", id, entry.getKey(), e);
            errorCount.incrementAndGet();
        } catch (RuntimeException e) {
            JsonPatcher.RELOAD_LOGGER.error("Unexpected error while loading patches", e);
            errorCount.incrementAndGet();
        }
        return null;
    }

    private static void logParseError(Map.Entry<Identifier, Resource> entry, Parser.Result.Fail fail, Identifier id) {
        if (Config.MANAGER.get().useJavaStacktrace()) {
            var error = new RuntimeException();
            fail.errors().forEach(error::addSuppressed);
            JsonPatcher.RELOAD_LOGGER.error("Failed to parse patch {} from {}:", id, entry.getKey(), error);
        } else {
            JsonPatcher.RELOAD_LOGGER.error("Failed to parse patch {} from {}:\n{}", id, entry.getKey(), fail.errors()
                    .stream()
                    .map(Parser.ParseException::toString)
                    .collect(Collectors.joining("\n")));
        }
    }

    @Nullable
    private static Patch validateAndBuild(Identifier id, Parser.Result.Success result) {
        var meta = result.metadata();
        if (meta.has("enabled") && !meta.getBoolean("enabled")) {
            return null;
        }

        if (!JsonPatcher.isSupportedVersion(meta.getString("version"))) {
            throw new IllegalStateException("Unsupported patch version '%s'".formatted(meta.getString("version")));
        }

        List<PatchTarget> target;
        if (meta.has("target")) {
            target = PatchTarget.LIST_CODEC.parse(ValueOps.INSTANCE, meta.get("target"))
                    .getOrThrow(false, error -> {
                        throw new IllegalStateException("Failed to parse target: %s".formatted(error));
                    });
        } else {
            target = List.of();
        }

        return new Patch(result.program(), id, target, meta.has("metapatch"));
    }
}
