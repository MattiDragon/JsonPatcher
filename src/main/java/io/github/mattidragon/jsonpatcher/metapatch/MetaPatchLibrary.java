package io.github.mattidragon.jsonpatcher.metapatch;

import com.google.gson.JsonObject;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.stdlib.DontBind;
import io.github.mattidragon.jsonpatcher.lang.runtime.stdlib.LibraryBuilder;
import io.github.mattidragon.jsonpatcher.misc.GsonConverter;
import io.github.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class MetaPatchLibrary {
    @DontBind
    private final Map<Identifier, JsonObject> addedFiles = new HashMap<>();
    @DontBind
    private final List<Identifier> deletedFiles = new ArrayList<>();
    @DontBind
    private final ResourceManager resourceManager;

    public MetaPatchLibrary(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @DontBind
    public void apply(MetaPatchResourcePack metaPack) {
        metaPack.set(addedFiles, deletedFiles);
    }

    public void addFile(LibraryBuilder.FunctionContext context, Value.StringValue idString, Value.ObjectValue file) {
        var id = Identifier.tryParse(idString.value());
        if (id == null) throw new EvaluationException("Invalid identifier: " + idString.value(), context.callPos());
        try {
            deletedFiles.remove(id);
            addedFiles.put(id, GsonConverter.toGson(file));
        } catch (IllegalStateException e) {
            throw new EvaluationException("Failed to convert to json: " + e.getMessage(), context.callPos());
        }
    }

    public void deleteFile(LibraryBuilder.FunctionContext context, Value.StringValue idString) {
        var id = Identifier.tryParse(idString.value());
        if (id == null) throw new EvaluationException("Invalid identifier: " + idString.value(), context.callPos());

        addedFiles.remove(id);
        deletedFiles.add(id);
    }

    public Value getFile(LibraryBuilder.FunctionContext context, Value.StringValue idString) {
        var id = Identifier.tryParse(idString.value());
        if (id == null) throw new EvaluationException("Invalid identifier: " + idString.value(), context.callPos());

        try (var __ = PatchingContext.disablePatching()) {
            var resource = resourceManager.getResource(id);
            if (resource.isPresent()) {
                try {
                    return GsonConverter.fromGson(MetaPatchResourcePack.GSON.fromJson(new InputStreamReader(resource.get().getInputStream()), JsonObject.class));
                } catch (IllegalStateException e) {
                    throw new EvaluationException("Failed to convert from json: " + e.getMessage(), context.callPos());
                } catch (IOException e) {
                    throw new EvaluationException("Failed to read file: " + e.getMessage(), context.callPos());
                }
            }
        }

        return Value.NullValue.NULL;
    }
}
