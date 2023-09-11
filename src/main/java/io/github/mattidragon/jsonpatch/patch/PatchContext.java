package io.github.mattidragon.jsonpatch.patch;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonWriter;
import io.github.mattidragon.jsonpatch.JsonPatch;
import io.github.mattidragon.jsonpatch.lang.runtime.Context;
import io.github.mattidragon.jsonpatch.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatch.lang.runtime.Value;
import io.github.mattidragon.jsonpatch.lang.runtime.VariableStack;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.Executor;

public class PatchContext {
    private static final ThreadLocal<Stored> ACTIVE = new ThreadLocal<>();
    private static final Gson GSON = new Gson();

    private boolean loaded = false;
    private PatchStorage patches = null;

    public PatchContext() {
    }

    public static PatchContext get() {
        var stored = ACTIVE.get();
        return stored == null ? null : stored.context;
    }

    public static void remove() {
        var stored = ACTIVE.get();
        if (stored == null) throw new IllegalStateException("State not set");
        if (stored.count > 1) {
            ACTIVE.set(new Stored(stored.context, stored.count - 1));
        } else {
            ACTIVE.remove();
        }
    }

    public static void set(PatchContext context) {
        var stored = ACTIVE.get();
        if (stored != null && stored.context != context) throw new IllegalStateException("State already set to different value");
        var prevCount = stored == null ? 0 : stored.count;
        ACTIVE.set(new Stored(context, prevCount + 1));
    }

    public void load(ResourceManager manager, Executor executor) {
        if (loaded) throw new IllegalStateException("Already loaded");
        patches = new PatchStorage(PatchLoader.load(executor, manager));
        JsonPatch.RELOAD_LOGGER.info("Loaded {} patches", patches.size());
        loaded = true;
    }

    private boolean hasPatches(Identifier id) {
        return patches.hasPatches(id);
    }

    private void applyPatches(JsonElement json, Identifier id) {
        var errors = new ArrayList<EvaluationException>();
        for (var patch : patches.getPatches(id)) {
            try {
                patch.program().execute(new Context(new Value.ObjectValue(JsonHelper.asObject(json, "patched file")), new VariableStack()));
            } catch (EvaluationException e) {
                errors.add(e);
            }
        }
        if (!errors.isEmpty()) {
            JsonPatch.MAIN_LOGGER.error("Encountered {} error(s) while patching {}. See logs/jsonpatch.log for details", errors.size(), id);
            errors.forEach(error -> JsonPatch.RELOAD_LOGGER.error("Error while patching {}", id, error));
        }
    }

    public static InputSupplier<InputStream> patchInputStream(Identifier id, InputSupplier<InputStream> stream) {
        if (!id.getPath().endsWith(".json")) return stream;
        var context = get();
        if (context == null) {
            JsonPatch.RELOAD_LOGGER.warn("No state set when patching {}", id, new Throwable("Stacktrace"));
            return stream;
        }
        if (!context.loaded) throw new IllegalStateException("Context not loaded");

        if (!context.hasPatches(id)) return stream;

        try {
            var json = GSON.fromJson(new InputStreamReader(stream.get()), JsonElement.class);
            context.applyPatches(json, id);

            var out = new ByteArrayOutputStream();
            var writer = new OutputStreamWriter(out);
            GSON.toJson(json, new JsonWriter(writer));
            writer.close();

            JsonPatch.RELOAD_LOGGER.debug("Patching {}", id);
            return () -> new ByteArrayInputStream(out.toByteArray());
        } catch (JsonParseException | IOException e) {
            JsonPatch.RELOAD_LOGGER.error("Failed to patch json at {}", id, e);
            return stream;
        }
    }

    private record Stored(PatchContext context, int count) {
    }
}
