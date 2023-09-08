package io.github.mattidragon.jsonpatch;

import io.github.mattidragon.jsonpatch.lang.parse.Lexer;
import io.github.mattidragon.jsonpatch.lang.parse.Parser;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class PatchLoader {
    private static final ResourceFinder finder = new ResourceFinder("jsonpatch", ".jsonpatch");

    public static List<Patch> load(Executor executor, ResourceManager manager) {
        var files = finder.findResources(manager);
        var futures = new ArrayList<CompletableFuture<Void>>();
        var patches = Collections.synchronizedList(new ArrayList<Patch>());
        for (var entry : files.entrySet()) {
            futures.add(CompletableFuture.runAsync(() -> {
                var id = finder.toResourceId(entry.getKey());
                var resource = entry.getValue();

                try {
                    var code = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    var lexResult = new Lexer(code, id.toString()).lex();
                    var meta = lexResult.metadata();
                    var target = getTarget(meta);

                    var program = new Parser(lexResult.tokens()).program();
                    patches.add(new Patch(program, target));
                } catch (IOException | Lexer.LexException | Parser.ParseException | IllegalStateException e) {
                    JsonPatch.RELOAD_LOGGER.error("Failed to load patch {} from {}", id, entry.getKey(), e);
                }
            }, executor));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return patches;
    }

    private static Identifier getTarget(Map<String, Optional<String>> meta) {
        if (!meta.containsKey("target") || meta.get("target").isEmpty()) {
            throw new IllegalStateException("Missing or empty target");
        }
        var target = Identifier.tryParse(meta.get("target").get());
        if (target == null) throw new IllegalStateException("Invalid target");
        return target;
    }
}
