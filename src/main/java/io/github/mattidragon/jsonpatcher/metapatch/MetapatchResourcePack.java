package io.github.mattidragon.jsonpatcher.metapatch;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.resource.*;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.function.Predicate;

public class MetapatchResourcePack implements ResourcePack {
    public static final Gson GSON = new Gson();

    public final ResourceType type;
    private final Map<Identifier, JsonObject> files = new HashMap<>();
    private final List<FileFilter> filters = new ArrayList<>();
    private final Set<String> namespaces = new HashSet<>();

    public MetapatchResourcePack(ResourceType type) {
        this.type = type;
    }

    public void clear() {
        files.clear();
        filters.clear();
        namespaces.clear();
    }

    public void set(Map<Identifier, JsonObject> files, List<FileFilter> deletedFiles) {
        this.files.clear();
        this.files.putAll(files);
        this.filters.clear();
        this.filters.addAll(deletedFiles);
        namespaces.clear();
        files.keySet().forEach(id -> namespaces.add(id.getNamespace()));
    }

    public boolean isDeleted(Identifier id) {
        // The last filter added will get priority
        for (var filter : filters.reversed()) {
            if (filter.target().test(id)) {
                return !filter.allow();
            }
        }
        return false;
    }

    public Map<Identifier, Resource> findResources(String startingPath, Predicate<Identifier> allowedPathPredicate) {
        var map = new HashMap<Identifier, Resource>();
        files.forEach((id, file) -> {
            if (id.getPath().startsWith(startingPath) && allowedPathPredicate.test(id)) {
                map.put(id, makeResource(id));
            }
        });
        return map;
    }

    @Nullable
    @Override
    public InputSupplier<InputStream> openRoot(String... segments) {
        return null;
    }

    @Nullable
    @Override
    public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
        if (type != this.type) return null;
        var file = files.get(id);
        if (file == null) return null;

        return () -> {
            var out = new ByteArrayOutputStream();
            var writer = new OutputStreamWriter(out);
            GSON.toJson(file, writer);
            writer.close();
            return new ByteArrayInputStream(out.toByteArray());
        };
    }

    @Override
    public void findResources(ResourceType type, String namespace, String prefix, ResultConsumer consumer) {
        if (type != this.type) return;

        files.forEach((id, file) -> {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix)) {
                consumer.accept(id, open(type, id));
            }
        });
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        return namespaces;
    }

    @Nullable
    @Override
    public <T> T parseMetadata(ResourceMetadataReader<T> metaReader) {
        var metadata = getMetadata(type);
        var stream = new ByteArrayInputStream(metadata.getBytes());

        return AbstractFileResourcePack.parseMetadata(metaReader, stream);
    }

    @Override
    public ResourcePackInfo getInfo() {
        return new ResourcePackInfo("jsonpatcher:meta_patch", 
                Text.literal("JsonPatcher MetaPatch Resource Pack"), 
                ResourcePackSource.BUILTIN, 
                Optional.empty());
    }

    @Override
    public void close() {

    }

    private static String getMetadata(ResourceType type) {
        return """
            {
              "pack": {
                "pack_format": %s,
                "description": "JsonPatcher MetaPatch Resource Pack"
              }
            }
            """.formatted(SharedConstants.getGameVersion().getResourceVersion(type));
    }

    @Nullable
    public Resource makeResource(Identifier id) {
        var supplier = open(type, id);
        if (supplier != null) {
            return new Resource(this, supplier);
        }
        return null;
    }
}
