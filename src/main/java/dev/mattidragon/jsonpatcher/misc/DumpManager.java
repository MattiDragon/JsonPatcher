package dev.mattidragon.jsonpatcher.misc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.mattidragon.jsonpatcher.JsonPatcher;
import dev.mattidragon.jsonpatcher.config.Config;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

public class DumpManager {
    private static final Gson DUMP_GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static void dumpIfEnabled(Identifier id, PackType packType, JsonElement patchedData) {
        if (Config.MANAGER.get().dumpPatchedFiles()) {
            try {
                var file = getDumpPath(packType.getDirectory())
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

    public static void cleanDump(String dumpLocation) {
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

    public static Path getDumpPath(String dumpLocation) {
        return FabricLoader.getInstance().getGameDir()
                .resolve("jsonpatcher/dump")
                .resolve(dumpLocation);
    }
}
