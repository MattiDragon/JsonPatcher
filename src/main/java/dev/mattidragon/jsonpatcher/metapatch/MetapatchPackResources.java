package dev.mattidragon.jsonpatcher.metapatch;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import dev.mattidragon.jsonpatcher.trust.TrustProvider;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

public class MetapatchPackResources implements PackResources, TrustProvider {
    public static final Gson GSON = new Gson();

    public final PackType type;
    private final Map<Identifier, JsonObject> files = new HashMap<>();
    private final List<FileFilter> filters = new ArrayList<>();
    private final Set<String> namespaces = new HashSet<>();

    private @Nullable ResourceMetadata metadata;

    public MetapatchPackResources(PackType type) {
        super();
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
        files.forEach((id, _) -> {
            if (id.getPath().startsWith(startingPath) && allowedPathPredicate.test(id)) {
                map.put(id, Objects.requireNonNull(makeResource(id), "this should exist"));
            }
        });
        return map;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... segments) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
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
    public void listResources(PackType type, String namespace, String prefix, ResourceOutput consumer) {
        if (type != this.type) return;

        files.forEach((id, _) -> {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix)) {
                consumer.accept(id, Objects.requireNonNull(getResource(type, id), "this should exist"));
            }
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return namespaces;
    }

    @Override
    public @Nullable <T> T getMetadataSection(MetadataSectionType<T> metadataSerializer) throws IOException {
        if (metadata == null) {
            metadata = ResourceMetadata.fromJsonStream(new ByteArrayInputStream(getMetadata(type).getBytes(StandardCharsets.UTF_8)));
        }

        return metadata.getSection(metadataSerializer).orElse(null);
    }

    @Override
    public PackLocationInfo location() {
        return new PackLocationInfo(
                "jsonpatcher:meta_patch",
                Component.literal("JsonPatcher MetaPatch Resource Pack"),
                PackSource.BUILT_IN,
                Optional.empty()
        );
    }

    @Override
    public void close() {

    }

    private static String getMetadata(PackType type) {
        return """
            {
              "pack": {
                "pack_format": %s,
                "description": "JsonPatcher MetaPatch Resource Pack"
              }
            }
            """.formatted(SharedConstants.getCurrentVersion().packVersion(type));
    }

    public @Nullable Resource makeResource(Identifier id) {
        var supplier = getResource(type, id);
        if (supplier != null) {
            return new Resource(this, supplier);
        }
        return null;
    }

    @Override
    public TrustLevel jsonpatcher$trustLevel() {
        // Metapatching can be done by untrusted code
        // Patches should never load from this pack, but just to be safe:
        return TrustLevel.UNTRUSTED;
    }
}
