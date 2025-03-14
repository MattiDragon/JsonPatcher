package io.github.mattidragon.jsonpatcher.patch;

import io.github.mattidragon.jsonpatcher.JsonPatcher;
import io.github.mattidragon.jsonpatcher.misc.DumpManager;
import io.github.mattidragon.jsonpatcher.misc.ResourceAccess;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

public class PatchingContext {
    private static final ThreadLocal<Unit> DISABLED = new ThreadLocal<>();

    private final ResourceType resourceType;
    private boolean loaded = false;
    private Patcher patcher = null;

    public PatchingContext(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public static Enabler disablePatching() {
        return new Enabler();
    }

    public void load(ResourceManager manager) {
        if (loaded) throw new IllegalStateException("Already loaded");

        var patches = PatchLoader.loadPatches(Patcher.PATCH_RUNNER, manager, resourceType);
        DumpManager.cleanDump(resourceType.getDirectory());

        JsonPatcher.RELOAD_LOGGER.info("Loaded {} patches for reload {}", patches.size(), resourceType.name());

        patcher = new Patcher(resourceType, patches);
        patcher.runMetaPatches(manager, Patcher.PATCH_RUNNER);
        loaded = true;
    }

    public void patchResource(Identifier id, Resource resource) {
        if (!id.getPath().endsWith(".json")) return;
        if (DISABLED.get() != null) return;
        
        if (!loaded) throw new IllegalStateException("Context not loaded");
        if (patcher.hasPatches(id)) {
            ((ResourceAccess) resource).jsonpatcher$disableKnowPack();
            ((ResourceAccess) resource).jsonpatcher$modifyInputStreamSupplier(stream -> patcher.patchInputStream(id, stream));
        }
    }

    public static class Enabler implements AutoCloseable {
        private Enabler() {
            DISABLED.set(Unit.INSTANCE);
        }

        @Override
        public void close() {
            DISABLED.remove();
        }
    }
}
