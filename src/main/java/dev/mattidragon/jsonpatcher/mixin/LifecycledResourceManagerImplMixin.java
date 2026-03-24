package dev.mattidragon.jsonpatcher.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.mattidragon.jsonpatcher.metapatch.MetapatchResourcePack;
import dev.mattidragon.jsonpatcher.misc.MetaPatchPackAccess;
import dev.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Predicate;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Mixin(MultiPackResourceManager.class)
public class LifecycledResourceManagerImplMixin implements MetaPatchPackAccess {
    @Unique
    private MetapatchResourcePack jsonpatcher$metaPatchPack;
    @Unique
    private PatchingContext jsonpatcher$context;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Object;<init>()V", shift = At.Shift.AFTER, remap = false))
    private void init(PackType type, List<PackResources> packs, CallbackInfo ci) {
        jsonpatcher$metaPatchPack = new MetapatchResourcePack(type);
        jsonpatcher$context = new PatchingContext(type);
    }
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void initPatches(PackType type, List<PackResources> packs, CallbackInfo ci) {
        jsonpatcher$context.load((ResourceManager) this);
    }

    @ModifyReturnValue(method = "getNamespaces", at = @At("RETURN"))
    private Set<String> patchNamespaceSet(Set<String> value) {
        var set = new HashSet<>(value);
        set.addAll(jsonpatcher$metaPatchPack.getNamespaces(jsonpatcher$metaPatchPack.type));
        return set;
    }
    
    @ModifyReturnValue(method = "getResourceStack", at = @At("RETURN"))
    private List<Resource> injectResourcesIntoGetAll(List<Resource> original, ResourceLocation id) {
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
    private Optional<Resource> injectResourcesIntoGet(Optional<Resource> original, ResourceLocation id) {
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

    @ModifyReturnValue(method = "listResources", at = @At("RETURN"))
    private Map<ResourceLocation, Resource> injectResourcesIntoFind(Map<ResourceLocation, Resource> map, String startingPath, Predicate<ResourceLocation> allowedPathPredicate) {
        map.putAll(jsonpatcher$metaPatchPack.findResources(startingPath, allowedPathPredicate));
        map.keySet().removeIf(jsonpatcher$metaPatchPack::isDeleted);
        map.forEach(jsonpatcher$context::patchResource);
        return map;
    }

    @ModifyReturnValue(method = "listResourceStacks", at = @At("RETURN"))
    private Map<ResourceLocation, List<Resource>> injectResourcesIntoFindAll(Map<ResourceLocation, List<Resource>> map, String startingPath, Predicate<ResourceLocation> allowedPathPredicate) {
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
