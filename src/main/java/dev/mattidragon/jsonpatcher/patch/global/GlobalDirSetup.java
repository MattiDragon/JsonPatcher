package dev.mattidragon.jsonpatcher.patch.global;

import dev.mattidragon.jsonpatcher.JsonPatcher;
import dev.mattidragon.jsonpatcher.lang.stdlib.Stdlib;

import java.io.IOException;
import java.nio.file.Files;

public class GlobalDirSetup {
    private GlobalDirSetup() {
    }

    public static void setupDirs() {
        try {
            setupScriptsDir();
            dumpDocsDir();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to setup jsonpatcher files", e);
        }
    }

    private static void setupScriptsDir() throws IOException {
        var scriptsDir = JsonPatcher.DATA_DIR.resolve("scripts");
        if (Files.exists(scriptsDir)) return;

        Files.createDirectories(scriptsDir);
        Files.writeString(scriptsDir.resolve("README.md"), """
                # JsonPatcher global scripts
                Any jsonpatcher scripts placed here will be loaded on startup.
                
                If a script has the `@library` meta tag it will be available to all patches
                from elsewhere with a prefix of `scripts:global:` for imports.
                
                If a script has the `@init "main";` meta tag it will be executed during mod init.
                If it has the `@init "client";` meta tag it will be executed during client mod init.
                
                Under default config scripts present here will have access to the reflection library,
                allowing them to interact with Java code freely.
                """);
        Files.writeString(scriptsDir.resolve("example.jsonpatch"), """
                @version "2";
                @init "main";
                
                import "reflection";
                
                val ResourceLocation = reflection.findClass("net.minecraft.resources.ResourceLocation");
                
                val id1 = ResourceLocation.withDefaultNamespace("test_id");
                val id2 = ResourceLocation.fromNamespaceAndPath("minecraft", "test_id");
                
                debug.assert(id1.equals(id2));
                debug.log("Logging from example global script: " + strings.asString(id1));
                """);
    }

    private static void dumpDocsDir() throws IOException {
        var docsDir = JsonPatcher.DATA_DIR.resolve("docs");
        Files.createDirectories(docsDir);
        Files.writeString(docsDir.resolve("README.md"), """
                # JsonPatcher doc files
                This directory contains copies of the standard library.
                It gets regenerated each time the game is launched and modifications won't be respected.
                
                Use this as a reference or let your editor plugins read the doc comments.
                """);

        for (var name : Stdlib.GLOBAL_LIBRARY_NAMES) {
            Files.writeString(docsDir.resolve(name + ".jsonpatch"), Stdlib.LIBRARY_CONTENTS.get(name));
        }
        for (var name : Stdlib.MISC_LIBRARY_NAMES) {
            Files.writeString(docsDir.resolve(name + ".jsonpatch"), Stdlib.LIBRARY_CONTENTS.get(name));
        }
    }
}
