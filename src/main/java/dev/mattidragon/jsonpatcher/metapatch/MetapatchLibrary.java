package dev.mattidragon.jsonpatcher.metapatch;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import dev.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import dev.mattidragon.jsonpatcher.lang.runtime.lib.builder.DontBind;
import dev.mattidragon.jsonpatcher.lang.runtime.value.Value;
import dev.mattidragon.jsonpatcher.misc.GsonConverter;
import dev.mattidragon.jsonpatcher.misc.ValueOps;
import dev.mattidragon.jsonpatcher.patch.PatchTarget;
import dev.mattidragon.jsonpatcher.patch.PatchingContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

@SuppressWarnings("unused")
public class MetapatchLibrary {
    @DontBind
    private final Map<ResourceLocation, JsonObject> addedFiles = new HashMap<>();
    @DontBind
    private final List<FileFilter> filters = new ArrayList<>();
    @DontBind
    private final ResourceManager resourceManager;

    public MetapatchLibrary(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @DontBind
    public void apply(MetapatchPackResources metaPack) {
        metaPack.set(addedFiles, filters);
    }

    @DontBind
    public void clear() {
        addedFiles.clear();
        filters.clear();
    }

    @DontBind
    private boolean isDeleted(ResourceLocation id) {
        // The last filter added will get priority
        for (var filter : filters.reversed()) {
            if (filter.target().test(id)) {
                return !filter.allow();
            }
        }
        return false;
    }

    @DontBind
    private static Value.ObjectValue valueFromResource(Resource resource) throws IOException {
        return GsonConverter.fromGson(MetapatchPackResources.GSON.fromJson(new InputStreamReader(resource.open()), JsonObject.class));
    }

    public void addFile(EvaluationContext context, Value.StringValue idString, Value.ObjectValue file) {
        var id = ResourceLocation.parse(idString.value());

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
        var id = ResourceLocation.parse(idString.value());

        filters.add(new FileFilter(
                new PatchTarget(
                        Optional.of(id.getNamespace()),
                        Optional.of(new PatchTarget.Path(Either.left(id.getPath()))),
                        Optional.empty()),
                false));
    }

    public void deleteFiles(EvaluationContext context, Value targetValue) {
        var target = PatchTarget.CODEC.decode(ValueOps.INSTANCE, targetValue)
                .getOrThrow(error -> new IllegalStateException("Failed to parse target: " + error))
                .getFirst();
        filters.add(new FileFilter(target, false));
    }

    public Value getFile(EvaluationContext context, Value.StringValue idString) {
        var id = ResourceLocation.parse(idString.value());

        try (var __ = PatchingContext.disablePatching()) {
            var resource = resourceManager.getResource(id);
            if (resource.isPresent()) {
                return valueFromResource(resource.get());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return Value.NullValue.NULL;
    }

    public Value getFiles(EvaluationContext context, Value.StringValue idString) {
        var id = ResourceLocation.parse(idString.value());

        var array = new Value.ArrayValue();
        try (var __ = PatchingContext.disablePatching()) {
            var resources = resourceManager.getResourceStack(id);
            for (var resource : resources) {
                array.value().add(valueFromResource(resource));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return array;
    }

    public Value searchFiles(EvaluationContext context, Value targetValue) {
        var target = PatchTarget.CODEC.decode(ValueOps.INSTANCE, targetValue)
                .getOrThrow(error -> new IllegalStateException("Failed to parse target: " + error))
                .getFirst();

        var startingPath = "";
        if (target.path().isPresent()) {
            startingPath = target.path()
                    .get()
                    .path()
                    .map(p -> p, Pair::getFirst);
        }
        // Remove last path segment, as minecraft treats it differently
        var slashIndex = startingPath.lastIndexOf('/');
        if (slashIndex != -1) {
            startingPath = startingPath.substring(0, slashIndex);
        }

        var out = new Value.ObjectValue();
        try (var __ = PatchingContext.disablePatching()) {
            var found = resourceManager.listResources(startingPath, target);
            for (var entry : found.entrySet()) {
                var id = entry.getKey();
                var resource = entry.getValue();
                out.value().put(id.toString(), valueFromResource(resource));
            }
        } catch (Exception e) {
            throw new UncheckedIOException(new IOException("Failed to search files", e));
        }

        return out;
    }
}
