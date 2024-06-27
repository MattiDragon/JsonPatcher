package io.github.mattidragon.jsonpatcher.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.mattidragon.jsonpatcher.metapatch.MetapatchResourcePack;
import io.github.mattidragon.jsonpatcher.misc.MetaPatchPackAccess;
import io.github.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.resource.*;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Predicate;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Mixin(LifecycledResourceManagerImpl.class)
public class LifecycledResourceManagerImplMixin implements MetaPatchPackAccess {
    @Unique
    private MetapatchResourcePack jsonpatcher$metaPatchPack;
    @Unique
    private PatchingContext jsonpatcher$context;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Object;<init>()V", shift = At.Shift.AFTER, remap = false))
    private void init(ResourceType type, List<ResourcePack> packs, CallbackInfo ci) {
        jsonpatcher$metaPatchPack = new MetapatchResourcePack(type);
        jsonpatcher$context = new PatchingContext(type);
    }
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void initPatches(ResourceType type, List<ResourcePack> packs, CallbackInfo ci) {
        jsonpatcher$context.load((ResourceManager) this);
    }

    @ModifyReturnValue(method = "getAllNamespaces", at = @At("RETURN"))
    private Set<String> patchNamespaceSet(Set<String> value) {
        var set = new HashSet<>(value);
        set.addAll(jsonpatcher$metaPatchPack.getNamespaces(jsonpatcher$metaPatchPack.type));
        return set;
    }
    
    @ModifyReturnValue(method = "getAllResources", at = @At("RETURN"))
    private List<Resource> injectResourcesIntoGetAll(List<Resource> original, Identifier id) {
        if (jsonpatcher$metaPatchPack.isDeleted(id)) {
            return new ArrayList<>();
        }
        var list = new ArrayList<>(original);
        var metaResource = jsonpatcher$metaPatchPack.makeResource(id);
        if (metaResource != null) list.add(metaResource);
        list.forEach(resource -> jsonpatcher$context.patchResource(id, resource));
        return list;
    }
    
    @ModifyReturnValue(method = "getResource", at = @At("RETURN"))
    private Optional<Resource> injectResourcesIntoGet(Optional<Resource> original, Identifier id) {
        if (jsonpatcher$metaPatchPack.isDeleted(id)) {
            return Optional.empty();
        }
        return Optional.ofNullable(jsonpatcher$metaPatchPack.makeResource(id))
                .or(() -> original)
                .map(resource -> {
                    jsonpatcher$context.patchResource(id, resource);
                    return resource;
                });
    }

    @ModifyReturnValue(method = "findResources", at = @At("RETURN"))
    private Map<Identifier, Resource> injectResourcesIntoFind(Map<Identifier, Resource> map, String startingPath, Predicate<Identifier> allowedPathPredicate) {
        map.putAll(jsonpatcher$metaPatchPack.findResources(startingPath, allowedPathPredicate));
        map.keySet().removeIf(jsonpatcher$metaPatchPack::isDeleted);
        map.forEach(jsonpatcher$context::patchResource);
        return map;
    }

    @ModifyReturnValue(method = "findAllResources", at = @At("RETURN"))
    private Map<Identifier, List<Resource>> injectResourcesIntoFindAll(Map<Identifier, List<Resource>> map, String startingPath, Predicate<Identifier> allowedPathPredicate) {
        jsonpatcher$metaPatchPack.findResources(startingPath, allowedPathPredicate)
                .forEach((id, resource) -> map.computeIfAbsent(id, i -> new ArrayList<>()).add(resource));
        map.keySet().removeIf(jsonpatcher$metaPatchPack::isDeleted);
        map.forEach((id, resources) -> resources.forEach(resource -> jsonpatcher$context.patchResource(id, resource)));
        return map;
    }

    @Override
    public MetapatchResourcePack jsonpatcher$getMetaPatchPack() {
        return jsonpatcher$metaPatchPack;
    }
}
