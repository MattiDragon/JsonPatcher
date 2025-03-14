package io.github.mattidragon.jsonpatcher.patch.global;

import dev.mattidragon.jsonpatcher.lang.runtime.bytecode.compiler.CompilerOptions;
import dev.mattidragon.jsonpatcher.lang.runtime.environment.EvaluationEnvironment;
import io.github.mattidragon.jsonpatcher.JsonPatcher;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class GlobalPatchLoader {
    private final EvaluationEnvironment environment = new EvaluationEnvironment(CompilerOptions.DEFAULT);

    private static Map<String, Path> findSources() {
        var sources = new HashMap<String, Path>();
        sources.put("scripts:", JsonPatcher.DATA_DIR.resolve("scripts"));
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            mod.findPath("jsonpatcher/scripts")
                    .ifPresent(path -> sources.put("mod:" + mod.getMetadata().getId(), path));
        }
        return sources;
    }

    public static void setupDirs() {
        try {
            var scriptsDir = JsonPatcher.DATA_DIR.resolve("scripts");
            if (!Files.exists(scriptsDir)) {
                Files.createDirectories(scriptsDir);
                Files.writeString(scriptsDir.resolve("README.md"), """
                        # JsonPatcher global scripts
                        Any jsonpatch scripts placed here will be loaded on startup.
                        
                        If a script has the `@library` meta tag it will be available to all patches
                        from elsewhere with a prefix of `scripts::` for imports.
                        
                        If a script has the `@init "main";` meta tag it will be executed during mod init.
                        If it has the `@init "client";` meta tag it will be executed during client mod init.
                        """);
            }
            dumpDocs();
        } catch (IOException e) {
            // TODO: handle
        }
    }

    private static void dumpDocs() throws IOException {
        var docsDir = JsonPatcher.DATA_DIR.resolve("docs");
        Files.createDirectories(docsDir);
        Files.writeString(docsDir.resolve("README.md"), """
                # JsonPatcher doc files
                This directory will in the future contain docs and code for the standard library.
                """);
    }
}
