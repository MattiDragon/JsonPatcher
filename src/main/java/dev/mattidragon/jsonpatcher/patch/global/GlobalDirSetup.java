package dev.mattidragon.jsonpatcher.patch.global;

import dev.mattidragon.jsonpatcher.JsonPatcher;
import dev.mattidragon.jsonpatcher.lang.stdlib.Stdlib;
import net.fabricmc.loader.api.FabricLoader;
import org.intellij.lang.annotations.Language;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class GlobalDirSetup {
    private GlobalDirSetup() {
    }

    public static void setupDirs() {
        try {
            setupScriptsDir();
            setupGlobalPatchesDir();
            dumpDocsDir();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to setup jsonpatcher files", e);
        }
    }

    private static void setupScriptsDir() throws IOException {
        var scriptsDir = JsonPatcher.DATA_DIR.resolve("scripts");
        var exists = Files.exists(scriptsDir);

        Files.createDirectories(scriptsDir);

        @Language("markdown")
        var readme = """
                # JsonPatcher global scripts
                Any jsonpatcher scripts placed here will be loaded on startup.
                
                If a script has the `@library` meta tag it will be available to all patches
                from elsewhere with a prefix of `scripts:global:` for imports.
                
                If a script has the `@init "main";` meta tag it will be executed during mod init.
                If it has the `@init "client";` meta tag it will be executed during client mod init.
                
                Under default config scripts present here will have access to the reflection library,
                allowing them to interact with Java code freely.
                """;
        Files.writeString(scriptsDir.resolve("README.md"), readme);

        // TODO: read lang version of currently loaded lang api
        @Language("json")
        var workspaceJson = """
                {
                  "$schema": "https://raw.githubusercontent.com/MattiDragon/JsonPatcherLang/refs/heads/2.0/tools/lang-server/jsonpatcher-workspace.schema.json",
                  "schema_version": 1,
                  "lang_version": [2, 0, 0],
                  "allowed_library_groups": ["default", "jsonpatcher:reflection", "+jsonpatcher:global_scripts"]
                }
                """;
        Files.writeString(scriptsDir.resolve("jsonpatcher-workspace.json"), workspaceJson);

        // If the scripts directory already existed, we don't want to add examples
        if (exists) return;

        Files.writeString(scriptsDir.resolve("example.jsonpatch"), """
                @version "2";
                @init "main";
                
                import "reflection";
                
                val Identifier = reflection.findClass("net.minecraft.resources.Identifier");
                
                val id1 = Identifier.withDefaultNamespace("test_id");
                val id2 = Identifier.fromNamespaceAndPath("minecraft", "test_id");
                
                debug.assert(id1.equals(id2));
                debug.log("Logging from example global script: " + strings.asString(id1));
                """);
    }

    private static void setupGlobalPatchesDir() throws IOException {
        var dir = JsonPatcher.DATA_DIR.resolve("global_patches");

        Files.createDirectories(dir.resolve("data"));
        Files.createDirectories(dir.resolve("assets"));

        @Language("markdown")
        var readme = """
                # JsonPatcher global patches
                This directory is intended as a convenient place for modpack developers to place patches.
                Programs from here are automatically loaded together with patches from data- and resourcepacks.
                They're loaded straight from the `data` and `assets` subdirectories,
                for datapack and resourcepack patching respectively.
                
                The patches here have modpack level trust, so they get access to reflection by default.
                Be careful not to make permanent changes to the game state however, as they will run on every
                reload like normal patches.
                """;
        Files.writeString(dir.resolve("README.md"), readme);

        @Language("json")
        var dataWorkspaceJson = """
                {
                  "$schema": "https://raw.githubusercontent.com/MattiDragon/JsonPatcherLang/refs/heads/2.0/tools/lang-server/jsonpatcher-workspace.schema.json",
                  "schema_version": 1,
                  "lang_version": [2, 0, 0],
                  "allowed_library_groups": ["default", "jsonpatcher:reflection", "+jsonpatcher:minecraft_data"]
                }
                """;
        Files.writeString(dir.resolve("data").resolve("jsonpatcher-workspace.json"), dataWorkspaceJson);

        @Language("json")
        var assetsWorkspaceJson = """
                {
                  "$schema": "https://raw.githubusercontent.com/MattiDragon/JsonPatcherLang/refs/heads/2.0/tools/lang-server/jsonpatcher-workspace.schema.json",
                  "schema_version": 1,
                  "lang_version": [2, 0, 0],
                  "allowed_library_groups": ["default", "jsonpatcher:reflection", "+jsonpatcher:minecraft_assets"]
                }
                """;
        Files.writeString(dir.resolve("assets").resolve("jsonpatcher-workspace.json"), assetsWorkspaceJson);
    }

    private static void dumpDocsDir() throws IOException {
        var docsDir = JsonPatcher.DATA_DIR.resolve("docs");
        Files.createDirectories(docsDir);
        var stdlibDir = docsDir.resolve("stdlib");
        Files.createDirectories(stdlibDir);
        var modDir = docsDir.resolve("mod");
        Files.createDirectories(modDir);

        @Language("markdown")
        var readme = """
                # JsonPatcher doc files
                This directory contains copies of the standard library.
                It gets regenerated each time the game is launched and modifications won't be respected.
                
                Use this as a reference or let your editor plugins read the doc comments.
                """;
        Files.writeString(docsDir.resolve("README.md"), readme);

        @Language("json")
        var workspaceJson = """
                {
                  "$schema": "https://raw.githubusercontent.com/MattiDragon/JsonPatcherLang/refs/heads/2.0/tools/lang-server/jsonpatcher-workspace.schema.json",
                  "schema_version": 1,
                  "external_stdlib_needed": false,
                  "allowed_library_groups": ["default", "jsonpatcher:internals", "jsonpatcher:reflection"]
                }
                """;
        Files.writeString(docsDir.resolve("jsonpatcher-workspace.json"), workspaceJson);

        for (var name : Stdlib.GLOBAL_LIBRARY_NAMES) {
            Files.writeString(stdlibDir.resolve(name + ".jsonpatch"), Stdlib.LIBRARY_CONTENTS.get(name));
        }
        for (var name : Stdlib.MISC_LIBRARY_NAMES) {
            Files.writeString(stdlibDir.resolve(name + ".jsonpatch"), Stdlib.LIBRARY_CONTENTS.get(name));
        }
        for (var name : Stdlib.DOC_LIBRARY_NAMES) {
            Files.writeString(stdlibDir.resolve(name + ".jsonpatch"), Stdlib.LIBRARY_CONTENTS.get(name));
        }

        var modContainer = FabricLoader.getInstance().getModContainer(JsonPatcher.MOD_ID).orElseThrow();
        var internalDocsPath = modContainer.findPath("docs");
        if (internalDocsPath.isPresent()) {
            Files.walkFileTree(internalDocsPath.get(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    var relativePath = internalDocsPath.get().relativize(file);
                    // Relative paths cannot be used across filesystems, so we must convert to string first
                    var targetPath = modDir.resolve(relativePath.toString());
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
