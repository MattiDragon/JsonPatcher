package io.github.mattidragon.jsonpatch;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonWriter;
import io.github.mattidragon.jsonpatch.lang.ast.Program;
import io.github.mattidragon.jsonpatch.lang.parse.Lexer;
import io.github.mattidragon.jsonpatch.lang.parse.Parser;
import io.github.mattidragon.jsonpatch.lang.ast.Context;
import io.github.mattidragon.jsonpatch.lang.ast.Statement;
import io.github.mattidragon.jsonpatch.lang.ast.Value;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.io.*;
import java.util.List;
import java.util.concurrent.Executor;

public class PatchContext {
    private static final ThreadLocal<Stored> ACTIVE = new ThreadLocal<>();
    private static final Gson GSON = new Gson();

    private boolean loaded = false;
    private List<Patch> patches = null;

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
        patches = PatchLoader.load(executor, manager);
        JsonPatch.RELOAD_LOGGER.info("Loaded {} patches", patches.size());
        loaded = true;
    }

    private boolean hasPatches(Identifier id) {
        return patches.stream().map(Patch::target).anyMatch(id::equals);
    }

    private void applyPatches(JsonElement json, Identifier id) {
        for (Patch patch : patches) {
            if (patch.target().equals(id)) {
                patch.program().execute(new Context(new Value.ObjectValue(JsonHelper.asObject(json, "patched file"))));
            }
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
