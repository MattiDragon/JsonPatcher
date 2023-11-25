package io.github.mattidragon.jsonpatcher.patch;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonWriter;
import io.github.mattidragon.jsonpatcher.JsonPatcher;
import io.github.mattidragon.jsonpatcher.config.Config;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.stdlib.LibraryBuilder;
import io.github.mattidragon.jsonpatcher.metapatch.MetaPatchLibrary;
import io.github.mattidragon.jsonpatcher.misc.DumpManager;
import io.github.mattidragon.jsonpatcher.misc.GsonConverter;
import io.github.mattidragon.jsonpatcher.misc.MetaPatchPackAccess;
import io.github.mattidragon.jsonpatcher.misc.ReloadDescription;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Patcher {
    private static final ExecutorService PATCHING_EXECUTOR = new ThreadPoolExecutor(0,
            Integer.MAX_VALUE,
            5,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("JsonPatch Patcher (%s)").build());

    private static final Gson GSON = new Gson();
    private final ReloadDescription description;
    private final PatchStorage patches;

    public Patcher(ReloadDescription description, PatchStorage patches) {
        this.description = description;
        this.patches = patches;
    }

    private boolean hasPatches(Identifier id) {
        return patches.hasPatches(id);
    }

    private JsonElement applyPatches(JsonElement json, Identifier id) {
        var errors = new ArrayList<Exception>();
        var activeJson = new MutableObject<>(JsonHelper.asObject(json, "patched file"));
        try {
            for (var patch : patches.getPatches(id)) {
                var root = GsonConverter.fromGson(activeJson.getValue());
                var timeBeforePatch = System.nanoTime();
                var success = runPatch(patch, PATCHING_EXECUTOR, errors::add, patches, root, Settings.builder()
                        .target(id.toString())
                        .build());
                var timeAfterPatch = System.nanoTime();
                JsonPatcher.RELOAD_LOGGER.debug("Patched {} with {} in {}ms", id, patch.id(), (timeAfterPatch - timeBeforePatch) / 1e6);
                if (success) {
                    activeJson.setValue(GsonConverter.toGson(root));
                }
            }
        } catch (RuntimeException e) {
            errors.add(e);
        }
        if (!errors.isEmpty()) {
            errors.forEach(error -> JsonPatcher.RELOAD_LOGGER.error("Error while patching {}", id, error));
            var message = "Encountered %s error(s) while patching %s. See logs/jsonpatch.log for details".formatted(errors.size(), id);
            description.errorConsumer().accept(Text.literal(message).formatted(Formatting.RED));
            if (Config.MANAGER.get().abortOnFailure()) {
                throw new PatchingException(message);
            } else {
                JsonPatcher.MAIN_LOGGER.error(message);
            }
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
    public static boolean runPatch(Patch patch, Executor executor, Consumer<RuntimeException> errorConsumer, PatchStorage patchStorage, Value.ObjectValue root, Settings settings) {
        try {
            var context = buildContext(patch.id(), patchStorage, root, settings);
            CompletableFuture.runAsync(() -> patch.program().execute(context), executor)
                    .get(Config.MANAGER.get().patchTimeoutMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof EvaluationException cause) {
                errorConsumer.accept(cause);
            } else if (e.getCause() instanceof StackOverflowError cause) {
                errorConsumer.accept(new PatchingException("Stack overflow while applying patch %s".formatted(patch.id()), cause));
            } else {
                errorConsumer.accept(new RuntimeException("Unexpected error while applying patch %s".formatted(patch.id()), e));
            }
        } catch (InterruptedException e) {
            errorConsumer.accept(new PatchingException("Async error while applying patch %s".formatted(patch.id()), e));
        } catch (TimeoutException e) {
            errorConsumer.accept(new PatchingException("Timeout while applying patch %s. Check for infinite loops and increase the timeout in the config.".formatted(patch.id()), e));
        }
        return false;
    }

    private static EvaluationContext buildContext(Identifier patchId, EvaluationContext.LibraryLocator libraryLocator, Value.ObjectValue root, Settings settings) {
        var builder = EvaluationContext.builder();
        builder.root(root);
        builder.libraryLocator(libraryLocator);
        builder.debugConsumer(value -> JsonPatcher.RELOAD_LOGGER.info("Debug from {}: {}", patchId, value));
        builder.variable("_isLibrary", settings.isLibrary());
        builder.variable("_target", settings.targetAsValue());
        builder.variable("_isMetaPatch", settings.isMetaPatch());
        if (settings.isMetaPatch()) {
            builder.variable("metaPatch", new LibraryBuilder(MetaPatchLibrary.class, settings.metaPatchLibrary).build());
        }
        return builder.build();
    }

    public InputSupplier<InputStream> patchInputStream(Identifier id, InputSupplier<InputStream> stream) {
        if (!hasPatches(id)) return stream;

        try {
            JsonPatcher.RELOAD_LOGGER.debug("Patching {}", id);
            var json = GSON.fromJson(new InputStreamReader(stream.get()), JsonElement.class);
            json = applyPatches(json, id);

            var out = new ByteArrayOutputStream();
            var writer = new OutputStreamWriter(out);
            GSON.toJson(json, new JsonWriter(writer));
            writer.close();

            DumpManager.dumpIfEnabled(id, description, json);
            return () -> new ByteArrayInputStream(out.toByteArray());
        } catch (JsonParseException | IOException e) {
            JsonPatcher.RELOAD_LOGGER.error("Failed to patch json at {}", id, e);
            if (Config.MANAGER.get().abortOnFailure()) {
                throw new RuntimeException("Failed to patch json at %s".formatted(id), e);
            }
            return stream;
        }
    }

    public void runMetaPatches(ResourceManager manager, Executor executor) {
        if (!(manager instanceof MetaPatchPackAccess packAccess)) {
            JsonPatcher.MAIN_LOGGER.error("Failed to run meta patches: resource manager doesn't expose meta pack");
            return;
        }

        var metaPack = packAccess.jsonpatcher$getMetaPatchPack();
        metaPack.clear();

        var metaPatches = new ArrayList<>(patches.getMetaPatches());
        metaPatches.sort(Comparator.comparing(Patch::priority));
        var lib = new MetaPatchLibrary(manager);
        var errors = new ArrayList<RuntimeException>();

        try {
            for (var patch : metaPatches) {
                var timeBeforePatch = System.nanoTime();
                runPatch(patch, executor, errors::add, patches, new Value.ObjectValue(), Settings.builder()
                        .metaPatchLibrary(lib)
                        .build());
                var timeAfterPatch = System.nanoTime();
                JsonPatcher.RELOAD_LOGGER.debug("Ran meta patch {} in {}ms", patch.id(), (timeAfterPatch - timeBeforePatch) / 1e6);
            }
        } catch (RuntimeException e) {
            errors.add(e);
        }

        if (!errors.isEmpty()) {
            errors.forEach(error -> JsonPatcher.RELOAD_LOGGER.error("Error while running meta patch", error));
            var message = "Encountered %s error(s) while running meta patches. See logs/jsonpatch.log for details".formatted(errors.size());

            description.errorConsumer().accept(Text.literal(message).formatted(Formatting.RED));
            if (Config.MANAGER.get().abortOnFailure()) {
                throw new PatchingException(message);
            } else {
                JsonPatcher.MAIN_LOGGER.error(message);
            }
        }

        lib.apply(metaPack);
    }

    public record Settings(@Nullable String target, boolean isLibrary, @Nullable MetaPatchLibrary metaPatchLibrary) {
        public static Builder builder() {
            return new Builder();
        }

        public Value targetAsValue() {
            return target == null ? Value.NullValue.NULL : new Value.StringValue(target);
        }

        public boolean isMetaPatch() {
            return metaPatchLibrary != null;
        }

        public static class Builder {
            private @Nullable String target;
            private boolean isLibrary;
            private @Nullable MetaPatchLibrary metaPatchLibrary;

            public Builder target(String target) {
                this.target = target;
                return this;
            }

            public Builder library() {
                this.isLibrary = true;
                return this;
            }

            public Builder metaPatchLibrary(MetaPatchLibrary metaPatchLibrary) {
                this.metaPatchLibrary = metaPatchLibrary;
                return this;
            }

            public Settings build() {
                return new Settings(target, isLibrary, metaPatchLibrary);
            }
        }
    }
}
