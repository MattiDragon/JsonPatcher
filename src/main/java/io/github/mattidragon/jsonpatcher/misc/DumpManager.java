package io.github.mattidragon.jsonpatcher.misc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.github.mattidragon.jsonpatcher.JsonPatcher;
import io.github.mattidragon.jsonpatcher.config.Config;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

public class DumpManager {
    private static final Gson DUMP_GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static void dumpIfEnabled(Identifier id, ReloadDescription description, JsonElement patchedData) {
        if (Config.MANAGER.get().dumpPatchedFiles() && description.dumpPath() != null) {
            try {
                var file = getDumpPath(description.dumpPath())
                        .resolve(Path.of(id.getNamespace(), id.getPath().split("/")));
                Files.createDirectories(file.getParent());
                try (var writer = new OutputStreamWriter(Files.newOutputStream(file))) {
                    DUMP_GSON.toJson(patchedData, writer);
                }
            } catch (IOException e) {
                JsonPatcher.RELOAD_LOGGER.error("Failed to dump patched file {}", id, e);
            }
        }
    }

    public static void cleanDump(@Nullable String dumpLocation) {
        if (dumpLocation == null) return;

        var file = getDumpPath(dumpLocation);
        if (Files.exists(file)) {
            var errors = new ArrayList<IOException>();
            try (var stream = Files.walk(file)) {
                stream.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                errors.add(e);
                            }
                        });
                if (!errors.isEmpty()) {
                    var error = new IOException("Errors while deleting dumped files");
                    errors.forEach(error::addSuppressed);
                    throw error;
                }
            } catch (IOException e) {
                JsonPatcher.RELOAD_LOGGER.error("Failed to clean dump directory", e);
            }
        }
    }

    private static Path getDumpPath(String dumpLocation) {
        return FabricLoader.getInstance().getGameDir()
                .resolve("jsonpatcher-dump")
                .resolve(dumpLocation);
    }
}
