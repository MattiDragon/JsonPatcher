package io.github.mattidragon.jsonpatcher.patch;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonWriter;
import io.github.mattidragon.jsonpatcher.JsonPatcher;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.apache.commons.lang3.mutable.MutableObject;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.function.Consumer;

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
        if (stored != null && stored.context != context) {
            remove(); // Clean up in case this was caused by one-off error
            throw new IllegalStateException("State already set to different value");
        }
        var prevCount = stored == null ? 0 : stored.count;
        ACTIVE.set(new Stored(context, prevCount + 1));
    }

    public void load(ResourceManager manager, Executor executor) {
        if (loaded) throw new IllegalStateException("Already loaded");
        patches = new PatchStorage(PatchLoader.load(executor, manager));
        JsonPatcher.RELOAD_LOGGER.info("Loaded {} patches", patches.size());
        loaded = true;
    }

    private boolean hasPatches(Identifier id) {
        return patches.hasPatches(id);
    }

    private JsonElement applyPatches(JsonElement json, Identifier id) {
        var executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("JsonPatch Patcher (%s)").build());
        var errors = new ArrayList<Exception>();
        var activeJson = new MutableObject<>(JsonHelper.asObject(json, "patched file"));
        for (var patch : patches.getPatches(id)) {
            var root = new Value.ObjectValue(activeJson.getValue());
            var success = runPatch(patch, executor, errors::add, patches, root);
            if (success) {
                activeJson.setValue(root.toGson(null));
            }
        }
        if (!errors.isEmpty()) {
            JsonPatcher.MAIN_LOGGER.error("Encountered {} error(s) while patching {}. See logs/jsonpatch.log for details", errors.size(), id);
            errors.forEach(error -> JsonPatcher.RELOAD_LOGGER.error("Error while patching {}", id, error));
        }
        return activeJson.getValue();
    }

    /**
     * Runs a patch with proper error handling.
     * @param patch The patch to run.
     * @param executor An executor to run the patch on, required to run on another thread for timeout to work
     * @param errorConsumer A consumer the receives errors from the patch.
     *                      Errors are either {@link EvaluationException EvaluationExceptions} for errors within the patch,
     *                      or {@link RuntimeException RuntimeExceptions} for timeouts and other errors not from the patch itself
     * @param patchStorage A patch storage for resolution of libraries
     * @param root The root object for the patch context, will be modified
     * @return {@code true} if the patch completed successfully. If {@code false} the {@code errorConsumer} should have received an error.
     */
    public static boolean runPatch(Patch patch, ExecutorService executor, Consumer<RuntimeException> errorConsumer, PatchStorage patchStorage, Value.ObjectValue root) {
        try {
            CompletableFuture.runAsync(() -> patch.program().execute(Context.create(root, patchStorage)), executor)
                    .get(200, TimeUnit.MILLISECONDS);
            return true;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof EvaluationException cause) {
                errorConsumer.accept(cause);
            } else if (e.getCause() instanceof StackOverflowError cause) {
                errorConsumer.accept(new RuntimeException("Stack overflow while applying patch %s".formatted(patch.id()), cause));
            } else {
                errorConsumer.accept(new RuntimeException("Unexpected error while applying patch %s".formatted(patch.id()), e));
            }
        } catch (InterruptedException e) {
            errorConsumer.accept(new RuntimeException("Async error while applying patch %s".formatted(patch.id()), e));
        } catch (TimeoutException e) {
            errorConsumer.accept(new RuntimeException("Timeout applying patch %s".formatted(patch.id()), e));
        }
        return false;
    }

    public static InputSupplier<InputStream> patchInputStream(Identifier id, InputSupplier<InputStream> stream) {
        if (!id.getPath().endsWith(".json")) return stream;
        var context = get();
        if (context == null) {
            JsonPatcher.RELOAD_LOGGER.warn("No state set when patching {}", id, new Throwable("Stacktrace"));
            return stream;
        }
        if (!context.loaded) throw new IllegalStateException("Context not loaded");

        if (!context.hasPatches(id)) return stream;

        try {
            var json = GSON.fromJson(new InputStreamReader(stream.get()), JsonElement.class);
            json = context.applyPatches(json, id);

            var out = new ByteArrayOutputStream();
            var writer = new OutputStreamWriter(out);
            GSON.toJson(json, new JsonWriter(writer));
            writer.close();

            JsonPatcher.RELOAD_LOGGER.debug("Patching {}", id);
            return () -> new ByteArrayInputStream(out.toByteArray());
        } catch (JsonParseException | IOException e) {
            JsonPatcher.RELOAD_LOGGER.error("Failed to patch json at {}", id, e);
            return stream;
        }
    }

    private record Stored(PatchContext context, int count) {
    }
}
