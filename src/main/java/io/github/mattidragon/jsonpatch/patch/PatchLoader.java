package io.github.mattidragon.jsonpatch.patch;

import io.github.mattidragon.jsonpatch.JsonPatch;
import io.github.mattidragon.jsonpatch.lang.parse.Lexer;
import io.github.mattidragon.jsonpatch.lang.parse.Parser;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public class PatchLoader {
    private static final ResourceFinder finder = new ResourceFinder("jsonpatch", ".jsonpatch");

    public static List<Patch> load(Executor executor, ResourceManager manager) {
        var files = finder.findResources(manager);
        var futures = new ArrayList<CompletableFuture<Void>>();
        var patches = Collections.synchronizedList(new ArrayList<Patch>());
        var errorCount = new AtomicInteger(0);
        for (var entry : files.entrySet()) {
            futures.add(CompletableFuture.runAsync(() -> {
                var id = finder.toResourceId(entry.getKey());
                var resource = entry.getValue();

                try {
                    var code = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    var lexResult = new Lexer(code, id.toString()).lex();
                    var meta = lexResult.metadata();
                    if (!meta.containsKey("version") || meta.get("version").isEmpty()) throw new IllegalStateException("Missing version declaration");
                    if (!meta.get("version").get().equals("1")) throw new IllegalStateException("Unsupported version: '%s'".formatted(meta.get("version").get()));

                    var target = getTarget(meta);

                    var program = new Parser(lexResult.tokens()).program();
                    patches.add(new Patch(program, target));
                } catch (IOException | Lexer.LexException | Parser.ParseException | IllegalStateException e) {
                    JsonPatch.RELOAD_LOGGER.error("Failed to load patch {} from {}", id, entry.getKey(), e);
                    errorCount.incrementAndGet();
                }
            }, executor));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        if (errorCount.get() > 0) {
            JsonPatch.MAIN_LOGGER.error("Failed to load {} patch(es). See logs/jsonpatch.log for details", errorCount.get());
        }
        return patches;
    }

    private static PatchTarget getTarget(Map<String, Optional<String>> meta) {
        if (!meta.containsKey("target") || meta.get("target").isEmpty()) {
            throw new IllegalStateException("Missing or empty target");
        }
        return PatchTarget.parse(meta.get("target").get());
    }
}
