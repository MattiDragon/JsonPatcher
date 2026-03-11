package dev.mattidragon.jsonpatcher.patch.global;

import dev.mattidragon.jsonpatcher.JsonPatcher;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class GlobalProgramScanner {
    public static final Path BASE_DIR = JsonPatcher.DATA_DIR.resolve("global_patches");

    private GlobalProgramScanner() {
    }

    public static Map<Identifier, InputSupplier<InputStream>> scan(ResourceType type) {
        var path = BASE_DIR.resolve(type.getDirectory());

        try (var stream = Files.walk(path)) {
            var out = new HashMap<Identifier, InputSupplier<InputStream>>();

            stream.filter(Files::isRegularFile)
                    .filter(patchPath -> patchPath.toString().endsWith(".jsonpatch"))
                    .forEach(patchPath -> {
                        var fileName = BASE_DIR.relativize(patchPath).toString();
                        var cleanedName = fileName.substring(0, fileName.length() - ".jsonpatch".length())
                                .replace(BASE_DIR.getFileSystem().getSeparator(), "/")
                                .replaceFirst("^\\./", "");
                        var id = Identifier.tryParse("global", "/" + cleanedName);
                        if (id == null) return;
                        out.put(id, InputSupplier.create(patchPath));
                    });

            return out;
        } catch (IOException e) {
            JsonPatcher.MAIN_LOGGER.error("Failed to scan global patches", e);
            return Map.of();
        }
    }
}
