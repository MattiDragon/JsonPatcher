package dev.mattidragon.jsonpatcher.patch;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonWriter;
import dev.mattidragon.jsonpatcher.JsonPatcher;
import dev.mattidragon.jsonpatcher.config.Config;
import dev.mattidragon.jsonpatcher.context.ProgramContext;
import dev.mattidragon.jsonpatcher.lang.runtime.value.Value;
import dev.mattidragon.jsonpatcher.misc.DumpManager;
import dev.mattidragon.jsonpatcher.misc.GsonConverter;
import dev.mattidragon.jsonpatcher.misc.MetaPatchPackAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.commons.lang3.mutable.MutableObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Patcher {
    public static final ExecutorService PATCH_RUNNER = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("JsonPatcher-Patch-Runner").factory());
    private static final Gson GSON = new Gson();
    private final PackType packType;
    private final PatchStorage patches;

    public Patcher(PackType packType, PatchStorage patches) {
        this.packType = packType;
        this.patches = patches;
    }

    public boolean hasPatches(Identifier id) {
        return patches.hasPatches(id);
    }

    private JsonElement applyPatches(JsonElement json, Identifier id) {
        var errors = new ArrayList<Exception>();
        var activeJson = new MutableObject<>(GsonHelper.convertToJsonObject(json, "patched file"));
        try {
            for (var patch : patches.getPatches(id)) {
                var root = GsonConverter.fromGson(activeJson.get());
                var timeBeforePatch = System.nanoTime();
                var success = runPatch(patch, PATCH_RUNNER, errors::add, root, () -> new ProgramContext.TargetContext("patch", id.toString()));
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
            var message = "Encountered %s error(s) while patching %s. See jsonpatcher/jsonpatcher.log for details".formatted(errors.size(), id);
            ErrorLogger.CURRENT.get().accept(Component.literal(message).withStyle(ChatFormatting.RED));
            if (Config.MANAGER.get().throwOnFailure()) {
                throw new PatchingException(message);
            } else {
                JsonPatcher.MAIN_LOGGER.error(message);
            }
        }
        return activeJson.get();
    }

    /**
     * Runs a patch with proper error handling.
     * @param patch The patch to run.
     * @param executor An executor to run the patch on, required to run on another thread for timeout to work
     * @param errorConsumer A consumer the receives errors from the patch.
     * @param root The root object for the patch context, will be modified
     * @return {@code true} if the patch completed successfully. If {@code false} the {@code errorConsumer} should have received an error.
     */
    public static boolean runPatch(LoadedProgram patch, Executor executor, Consumer<RuntimeException> errorConsumer, Value.ObjectValue root, Supplier<ProgramContext.RoleContext> contextSupplier) {
        try {
            CompletableFuture.runAsync(() -> {
                        try (var ignored = contextSupplier.get()) {
                            patch.program().run(root);
                        }
                    }, executor).get(Config.MANAGER.get().patchTimeoutMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                errorConsumer.accept(cause);
            } else if (e.getCause() instanceof StackOverflowError cause) {
                errorConsumer.accept(new PatchingException("Stack overflow while applying patch %s".formatted(patch.name()), cause));
            } else {
                errorConsumer.accept(new RuntimeException("Unexpected error while applying patch %s".formatted(patch.name()), e));
            }
        } catch (InterruptedException e) {
            errorConsumer.accept(new PatchingException("Async error while applying patch %s".formatted(patch.name()), e));
        } catch (TimeoutException e) {
            errorConsumer.accept(new PatchingException("Timeout while applying patch %s. Check for infinite loops and increase the timeout in the config.".formatted(patch.name()), e));
        }
        return false;
    }

    public IoSupplier<InputStream> patchInputStream(Identifier id, IoSupplier<InputStream> stream) {
        if (!hasPatches(id)) return stream;

        try {
            JsonPatcher.RELOAD_LOGGER.debug("Patching {}", id);
            var json = GSON.fromJson(new InputStreamReader(stream.get()), JsonElement.class);
            json = applyPatches(json, id);

            var out = new ByteArrayOutputStream();
            var writer = new OutputStreamWriter(out);
            GSON.toJson(json, new JsonWriter(writer));
            writer.close();

            DumpManager.dumpIfEnabled(id, packType, json);
            return () -> new ByteArrayInputStream(out.toByteArray());
        } catch (JsonParseException | IOException e) {
            if (Config.MANAGER.get().throwOnFailure()) {
                throw new RuntimeException("Failed to patch json at %s".formatted(id), e);
            } else {
                JsonPatcher.RELOAD_LOGGER.error("Failed to patch json at {}", id, e);
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
        patches.metapatchLibrary().clear();

        var metaPatches = new ArrayList<>(patches.getMetaPatches());
        metaPatches.sort(Comparator.comparing(Patch::priority));
        var errors = new ArrayList<RuntimeException>();

        try {
            for (var patch : metaPatches) {
                var timeBeforePatch = System.nanoTime();
                runPatch(patch, executor, errors::add, new Value.ObjectValue(), () -> new ProgramContext.RoleContext("metapatch"));
                var timeAfterPatch = System.nanoTime();
                JsonPatcher.RELOAD_LOGGER.debug("Ran meta patch {} in {}ms", patch.id(), (timeAfterPatch - timeBeforePatch) / 1e6);
            }
        } catch (RuntimeException e) {
            errors.add(e);
        }

        if (!errors.isEmpty()) {
            errors.forEach(error -> JsonPatcher.RELOAD_LOGGER.error("Error while running meta patch", error));
            var message = "Encountered %s error(s) while running meta patches. See jsonpatcher/jsonpatcher.log for details".formatted(errors.size());

            ErrorLogger.CURRENT.get().accept(Component.literal(message).withStyle(ChatFormatting.RED));
            if (Config.MANAGER.get().throwOnFailure()) {
                throw new PatchingException(message);
            } else {
                JsonPatcher.MAIN_LOGGER.error(message);
            }
        }
        patches.metapatchLibrary().apply(metaPack);
    }
}
