package io.github.mattidragon.jsonpatcher.metapatch;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MetapatchSingleResourceManager implements ResourceManager {
    private final Identifier id;
    @Nullable
    private final ResourceManager delegate;
    private final boolean delete;
    private final MetapatchResourcePack metaPatchPack;

    public MetapatchSingleResourceManager(Identifier id, @Nullable ResourceManager delegate, MetapatchResourcePack metaPatchPack) {
        this.id = id;
        this.delegate = delegate;
        this.metaPatchPack = metaPatchPack;
        this.delete = this.metaPatchPack.getDeletedFiles().contains(id);
    }

    @Override
    public Set<String> getAllNamespaces() {
        throw new UnsupportedOperationException("This method should not be called on this hack. Someone is doing something unexpected.");
    }

    @Override
    public List<Resource> getAllResources(Identifier id) {
        if (!this.id.equals(id)) {
            return delegate == null ? new ArrayList<>() : delegate.getAllResources(id);
        }

        if (delete) return new ArrayList<>();

        var resources = delegate == null ? new ArrayList<Resource>() : new ArrayList<>(delegate.getAllResources(id));
        var resource = metaPatchPack.makeResource(id);
        if (resource != null) resources.add(resource);
        return resources;
    }

    @Override
    public Optional<Resource> getResource(Identifier id) {
        if (!this.id.equals(id)) {
            return Optional.ofNullable(delegate).flatMap(rm -> rm.getResource(id));
        }

        if (delete) return Optional.empty();

        var resource = this.metaPatchPack.makeResource(id);
        if (resource != null) return Optional.of(resource);
        if (delegate != null) return delegate.getResource(id);
        return Optional.empty();
    }

    @Override
    public Map<Identifier, Resource> findResources(String startingPath, Predicate<Identifier> allowedPathPredicate) {
        throw new UnsupportedOperationException("This method should not be called on this hack. Someone is doing something unexpected.");
    }

    @Override
    public Map<Identifier, List<Resource>> findAllResources(String startingPath, Predicate<Identifier> allowedPathPredicate) {
        throw new UnsupportedOperationException("This method should not be called on this hack. Someone is doing something unexpected.");
    }

    @Override
    public Stream<ResourcePack> streamResourcePacks() {
        throw new UnsupportedOperationException("This method should not be called on this hack. Someone is doing something unexpected.");
    }
}
