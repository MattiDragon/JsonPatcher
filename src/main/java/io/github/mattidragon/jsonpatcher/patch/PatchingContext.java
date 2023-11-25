package io.github.mattidragon.jsonpatcher.patch;

import io.github.mattidragon.jsonpatcher.JsonPatcher;
import io.github.mattidragon.jsonpatcher.misc.DumpManager;
import io.github.mattidragon.jsonpatcher.misc.ReloadDescription;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.*;
import java.util.concurrent.*;

public class PatchingContext {
    private static final ThreadLocal<Stored> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> DISABLED = ThreadLocal.withInitial(() -> false);

    private final ReloadDescription description;
    private boolean loaded = false;
    private Patcher patcher = null;

    public PatchingContext(ReloadDescription description) {
        this.description = description;
    }

    public static Enabler disablePatching() {
        return new Enabler();
    }

    public static PatchingContext get() {
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

    public static void set(PatchingContext context) {
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

        var patches = PatchLoader.load(executor, manager);
        DumpManager.cleanDump(description.dumpPath());

        JsonPatcher.RELOAD_LOGGER.info("Loaded {} patches for reload '{}'", patches.size(), description.name());

        patcher = new Patcher(description, patches);
        patcher.runMetaPatches(manager, executor);
        loaded = true;
    }

    public static InputSupplier<InputStream> patchInputStream(Identifier id, InputSupplier<InputStream> stream) {
        if (!id.getPath().endsWith(".json")) return stream;

        if (DISABLED.get()) return stream;

        var context = get();
        if (context == null) {
            JsonPatcher.RELOAD_LOGGER.warn("No state set when patching {}", id, new Throwable("Stacktrace"));
            return stream;
        }
        if (!context.loaded) throw new IllegalStateException("Context not loaded");

        return context.patcher.patchInputStream(id, stream);
    }

    private record Stored(PatchingContext context, int count) {
    }

    public static class Enabler implements AutoCloseable {
        private Enabler() {
            DISABLED.set(true);
        }

        @Override
        public void close() {
            DISABLED.set(false);
        }
    }
}
