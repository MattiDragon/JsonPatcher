package dev.mattidragon.jsonpatcher.patch.global;

import dev.mattidragon.jsonpatcher.JsonPatcher;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;

public class GlobalProgramScanner {
    public static final Path BASE_DIR = JsonPatcher.DATA_DIR.resolve("global_patches");

    private GlobalProgramScanner() {
    }

    public static Map<ResourceLocation, IoSupplier<InputStream>> scan(PackType type) {
        var path = BASE_DIR.resolve(type.getDirectory());

        try (var stream = Files.walk(path)) {
            var out = new HashMap<ResourceLocation, IoSupplier<InputStream>>();

            stream.filter(Files::isRegularFile)
                    .filter(patchPath -> patchPath.toString().endsWith(".jsonpatch"))
                    .forEach(patchPath -> {
                        var fileName = BASE_DIR.relativize(patchPath).toString();
                        var cleanedName = fileName.substring(0, fileName.length() - ".jsonpatch".length())
                                .replace(BASE_DIR.getFileSystem().getSeparator(), "/")
                                .replaceFirst("^\\./", "");
                        var id = ResourceLocation.tryBuild("global", "/" + cleanedName);
                        if (id == null) return;
                        out.put(id, IoSupplier.create(patchPath));
                    });

            return out;
        } catch (IOException e) {
            JsonPatcher.MAIN_LOGGER.error("Failed to scan global patches", e);
            return Map.of();
        }
    }
}
