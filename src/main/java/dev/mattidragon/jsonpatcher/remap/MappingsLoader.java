package dev.mattidragon.jsonpatcher.remap;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import dev.mattidragon.jsonpatcher.JsonPatcher;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.minecraft.MinecraftVersion;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipInputStream;

public class MappingsLoader {
    private static final Gson GSON = new Gson();
    private static final Path INTERMEDIARY_DIR = JsonPatcher.DATA_DIR.resolve("cache/mappings/intermediary");
    private static final Path MOJMAP_DIR = JsonPatcher.DATA_DIR.resolve("cache/mappings/mojmap");
    private static final URI MANIFEST_URI;

    public static final MemoryMappingTree MAPPING_TREE = new MemoryMappingTree(true);

    static {
        try {
            MANIFEST_URI = new URI("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void init() {
        var mojmapPath = MOJMAP_DIR.resolve("%s_%s.txt".formatted(
                MinecraftVersion.CURRENT.getName(), FabricLoader.getInstance().getEnvironmentType().name().toLowerCase(Locale.ROOT)
        ));
        var intermediaryPath = INTERMEDIARY_DIR.resolve(MinecraftVersion.CURRENT.getName() + ".tiny");

        try {
            // TODO: download off thread
            if (!Files.exists(mojmapPath)) {
                JsonPatcher.MAIN_LOGGER.info("Downloading mojmap for jsonpatch remapping");
                downloadMojmap(mojmapPath);
            }
            if (!Files.exists(intermediaryPath)) {
                JsonPatcher.MAIN_LOGGER.info("Downloading intermediary for jsonpatch remapping");
                downloadIntermediary(intermediaryPath);
            }

            // TODO: merge mappings off thread
            var loadingTree = new MemoryMappingTree();
            loadingTree.setSrcNamespace("official");
            loadingTree.setDstNamespaces(List.of("intermediary", "named"));

            try (var reader = Files.newBufferedReader(intermediaryPath)) {
                Tiny2FileReader.read(reader, loadingTree);
            }
            try (var reader = Files.newBufferedReader(mojmapPath)) {
                ProGuardFileReader.read(reader, "named", "official", new MappingSourceNsSwitch(loadingTree, "official"));
            }

            MAPPING_TREE.setSrcNamespace("intermediary");
            MAPPING_TREE.setDstNamespaces(List.of("named"));
            loadingTree.accept(new MappingSourceNsSwitch(MAPPING_TREE, "intermediary"));
            MAPPING_TREE.setDstNamespaces(List.of("named"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load mappings", e);
        }
    }

    private static void downloadMojmap(Path path) {
        try (var httpClient = HttpClient.newHttpClient()) {
            Files.createDirectories(MOJMAP_DIR);

            var versionManifest = httpClient.send(
                    HttpRequest.newBuilder(MANIFEST_URI)
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            URI versionJsonUrl;
            try (var reader = GSON.newJsonReader(new InputStreamReader(versionManifest.body()))) {
                versionJsonUrl = extractVersionJsonUrl(reader);
            }

            var versionJson = httpClient.send(
                    HttpRequest.newBuilder(versionJsonUrl)
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            URI mappingsUrl;
            try (var reader = GSON.newJsonReader(new InputStreamReader(versionJson.body()))) {
                mappingsUrl = extractMappingsUrl(reader);
            }

            // Once this returns the file will be written
            httpClient.send(
                    HttpRequest.newBuilder(mappingsUrl)
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofFile(path)
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to download mappings", e);
        }
    }

    private static URI extractVersionJsonUrl(JsonReader reader) throws IOException, URISyntaxException {
        reader.beginObject();
        while (!reader.nextName().equals("versions")) {
            reader.skipValue();
        }
        reader.beginArray();

        while (reader.hasNext()) {
            reader.beginObject();
            String id = null;
            String url = null;
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "id" -> id = reader.nextString();
                    case "url" -> url = reader.nextString();
                    default -> reader.skipValue();
                }
            }
            reader.endObject();

            if (id != null && url != null && id.equals(MinecraftVersion.CURRENT.getName())) {
                return new URI(url);
            }
        }
        throw new IllegalStateException("Current version missing from manifest");
    }

    private static URI extractMappingsUrl(JsonReader reader) throws IOException, URISyntaxException {
        reader.beginObject();
        while (!reader.nextName().equals("downloads")) {
            reader.skipValue();
        }
        reader.beginObject();

        var target = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? "client_mappings" : "server_mappings";

        while (reader.hasNext()) {
            if (reader.nextName().equals(target)) {
                reader.beginObject();
                while (!reader.nextName().equals("url")) {
                    reader.skipValue();
                }
                return new URI(reader.nextString());
            } else {
                reader.skipValue();
            }
        }
        throw new IllegalStateException("Mappings not found in version json");
    }

    private static void downloadIntermediary(Path path) {
        try (var httpClient = HttpClient.newHttpClient()) {
            Files.createDirectories(path.getParent());
            var versionName = MinecraftVersion.CURRENT.getName();
            var url = new URI("https://maven.fabricmc.net/net/fabricmc/intermediary/%s/intermediary-%s-v2.jar".formatted(versionName, versionName));

            var jarResponse = httpClient.send(
                    HttpRequest.newBuilder(url)
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            try (var in = new ZipInputStream(jarResponse.body())) {
                while (true) {
                    var entry = in.getNextEntry();
                    if (entry == null) {
                        throw new IllegalStateException("Cannot find mappings in yarn jar");
                    }
                    if (entry.getName().endsWith(".tiny")) {
                        Files.copy(in, path);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to download intermediary mappings", e);
        }
    }
}
