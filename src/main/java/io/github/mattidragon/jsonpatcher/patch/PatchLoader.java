package io.github.mattidragon.jsonpatcher.patch;

import io.github.mattidragon.jsonpatcher.JsonPatcher;
import io.github.mattidragon.jsonpatcher.config.Config;
import io.github.mattidragon.jsonpatcher.lang.parse.*;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

                    var parseResult = new Parser(lexResult.tokens(), new PatchMetadata.ParserLookup()
                            .put(PatchMetadata.Target.KEY, PatchMetadata.Target.PARSER)
                            .put(PatchMetadata.Version.KEY, PatchMetadata.Version.PARSER)).program();

                    if (parseResult instanceof ParseResult.Fail fail) {
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
                        errorCount.incrementAndGet();
                    } else {
                        var result = (ParseResult.Success) parseResult;
                        var meta = result.metadata();
                        meta.expectSingle(PatchMetadata.Version.KEY); // We'll deal with validation once we have more than one supported version
                        var target = meta.expectSingleOrNone(PatchMetadata.Target.KEY);

                        patches.add(new Patch(result.program(), id, target.map(PatchMetadata.Target::target)));
                    }
                } catch (IOException | Lexer.LexException | IllegalStateException e) {
                    JsonPatcher.RELOAD_LOGGER.error("Failed to load patch {} from {}", id, entry.getKey(), e);
                    errorCount.incrementAndGet();
                }
            }, executor));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        if (errorCount.get() > 0) {
            JsonPatcher.MAIN_LOGGER.error("Failed to load {} patch(es). See logs/jsonpatch.log for details", errorCount.get());
        }
        return patches;
    }
}
