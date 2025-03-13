package io.github.mattidragon.jsonpatcher.metapatch;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import dev.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import dev.mattidragon.jsonpatcher.lang.runtime.lib.builder.DontBind;
import dev.mattidragon.jsonpatcher.lang.runtime.value.Value;
import io.github.mattidragon.jsonpatcher.misc.GsonConverter;
import io.github.mattidragon.jsonpatcher.misc.ValueOps;
import io.github.mattidragon.jsonpatcher.patch.PatchTarget;
import io.github.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.*;

@SuppressWarnings("unused")
public class MetapatchLibrary {
    @DontBind
    private final Map<Identifier, JsonObject> addedFiles = new HashMap<>();
    @DontBind
    private final List<FileFilter> filters = new ArrayList<>();
    @DontBind
    private final ResourceManager resourceManager;

    public MetapatchLibrary(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @DontBind
    public void apply(MetapatchResourcePack metaPack) {
        metaPack.set(addedFiles, filters);
    }

    @DontBind
    public void clear() {
        addedFiles.clear();
        filters.clear();
    }

    @DontBind
    private boolean isDeleted(Identifier id) {
        // The last filter added will get priority
        for (var filter : filters.reversed()) {
            if (filter.target().test(id)) {
                return !filter.allow();
            }
        }
        return false;
    }

    public void addFile(EvaluationContext context, Value.StringValue idString, Value.ObjectValue file) {
        var id = Identifier.of(idString.value());

        // Add filter to undo deletion if necessary
        if (isDeleted(id)) {
            filters.add(new FileFilter(
                    new PatchTarget(
                            Optional.of(id.getNamespace()),
                            Optional.of(new PatchTarget.Path(Either.left(id.getPath()))),
                            Optional.empty()),
                    true));
        }
        addedFiles.put(id, GsonConverter.toGson(file));
    }

    public void deleteFile(EvaluationContext context, Value.StringValue idString) {
        var id = Identifier.of(idString.value());

        filters.add(new FileFilter(
                new PatchTarget(
                        Optional.of(id.getNamespace()),
                        Optional.of(new PatchTarget.Path(Either.left(id.getPath()))),
                        Optional.empty()),
                false));
    }

    public void deleteFiles(EvaluationContext context, Value value) {
        var target = PatchTarget.CODEC.decode(ValueOps.INSTANCE, value)
                .getOrThrow(error -> new IllegalStateException("Failed to parse target: " + error))
                .getFirst();
        filters.add(new FileFilter(target, false));
    }

    public Value getFile(EvaluationContext context, Value.StringValue idString) {
        var id = Identifier.of(idString.value());

        try (var __ = PatchingContext.disablePatching()) {
            var resource = resourceManager.getResource(id);
            if (resource.isPresent()) {
                return GsonConverter.fromGson(MetapatchResourcePack.GSON.fromJson(new InputStreamReader(resource.get().getInputStream()), JsonObject.class));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return Value.NullValue.NULL;
    }

    public Value getFiles(EvaluationContext context, Value.StringValue idString) {
        var id = Identifier.of(idString.value());

        var array = new Value.ArrayValue();
        try (var __ = PatchingContext.disablePatching()) {
            var resources = resourceManager.getAllResources(id);
            for (var resource : resources) {
                var value = GsonConverter.fromGson(MetapatchResourcePack.GSON.fromJson(new InputStreamReader(resource.getInputStream()), JsonObject.class));
                array.value().add(value);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return array;
    }
}
