package io.github.mattidragon.jsonpatcher.metapatch;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.stdlib.DontBind;
import io.github.mattidragon.jsonpatcher.lang.runtime.stdlib.LibraryBuilder;
import io.github.mattidragon.jsonpatcher.misc.GsonConverter;
import io.github.mattidragon.jsonpatcher.misc.ValueOps;
import io.github.mattidragon.jsonpatcher.patch.PatchTarget;
import io.github.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStreamReader;
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
    private boolean isDeleted(Identifier id) {
        // The last filter added will get priority
        for (var filter : filters.reversed()) {
            if (filter.target().test(id)) {
                return !filter.allow();
            }
        }
        return false;
    }

    public void addFile(LibraryBuilder.FunctionContext context, Value.StringValue idString, Value.ObjectValue file) {
        var id = Identifier.tryParse(idString.value());
        if (id == null) throw new EvaluationException(context.context().config(), "Invalid identifier: " + idString.value(), context.callPos());
        try {
            if (isDeleted(id)) {
                filters.add(new FileFilter(
                        new PatchTarget(
                                Optional.of(id.getNamespace()), 
                                Optional.of(new PatchTarget.Path(Either.left(id.getPath()))), 
                                Optional.empty()), 
                        true));
            }
            addedFiles.put(id, GsonConverter.toGson(file));
        } catch (IllegalStateException e) {
            throw new EvaluationException(context.context().config(), "Failed to convert to json: " + e.getMessage(), context.callPos());
        }
    }

    public void deleteFile(LibraryBuilder.FunctionContext context, Value.StringValue idString) {
        var id = Identifier.tryParse(idString.value());
        if (id == null) throw new EvaluationException(context.context().config(), "Invalid identifier: " + idString.value(), context.callPos());

        filters.add(new FileFilter(
                new PatchTarget(
                        Optional.of(id.getNamespace()),
                        Optional.of(new PatchTarget.Path(Either.left(id.getPath()))),
                        Optional.empty()),
                false));
    }

    public void deleteFiles(LibraryBuilder.FunctionContext context, Value value) {
        var target = PatchTarget.CODEC.decode(ValueOps.INSTANCE, value)
                .getOrThrow(error -> new EvaluationException(context.context().config(), "Failed to parse target: " + error, context.callPos()))
                .getFirst();
        filters.add(new FileFilter(target, false));
    }

    public Value getFile(LibraryBuilder.FunctionContext context, Value.StringValue idString) {
        var id = Identifier.tryParse(idString.value());
        if (id == null) throw new EvaluationException(context.context().config(), "Invalid identifier: " + idString.value(), context.callPos());

        try (var __ = PatchingContext.disablePatching()) {
            var resource = resourceManager.getResource(id);
            if (resource.isPresent()) {
                try {
                    return GsonConverter.fromGson(MetapatchResourcePack.GSON.fromJson(new InputStreamReader(resource.get().getInputStream()), JsonObject.class));
                } catch (IllegalStateException e) {
                    throw new EvaluationException(context.context().config(), "Failed to convert from json: " + e.getMessage(), context.callPos());
                } catch (IOException e) {
                    throw new EvaluationException(context.context().config(), "Failed to read file: " + e.getMessage(), context.callPos());
                }
            }
        }

        return Value.NullValue.NULL;
    }
}
