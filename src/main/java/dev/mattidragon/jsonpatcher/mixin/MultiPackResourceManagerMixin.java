package dev.mattidragon.jsonpatcher.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.mattidragon.jsonpatcher.metapatch.MetapatchPackResources;
import dev.mattidragon.jsonpatcher.misc.MetaPatchPackAccess;
import dev.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.resources.Identifier;
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

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "NotNullFieldNotInitialized"})
@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin implements MetaPatchPackAccess {
    @Unique
    private MetapatchPackResources jsonpatcher$metaPatchPack;
    @Unique
    private PatchingContext jsonpatcher$context;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Object;<init>()V", shift = At.Shift.AFTER, remap = false))
    private void init(PackType type, List<PackResources> packs, CallbackInfo ci) {
        jsonpatcher$metaPatchPack = new MetapatchPackResources(type);
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

    @ModifyReturnValue(method = "listResources", at = @At("RETURN"))
    private Map<Identifier, Resource> injectResourcesIntoFind(Map<Identifier, Resource> map, String startingPath, Predicate<Identifier> allowedPathPredicate) {
        map.putAll(jsonpatcher$metaPatchPack.findResources(startingPath, allowedPathPredicate));
        map.keySet().removeIf(jsonpatcher$metaPatchPack::isDeleted);
        map.forEach(jsonpatcher$context::patchResource);
        return map;
    }

    @ModifyReturnValue(method = "listResourceStacks", at = @At("RETURN"))
    private Map<Identifier, List<Resource>> injectResourcesIntoFindAll(Map<Identifier, List<Resource>> map, String startingPath, Predicate<Identifier> allowedPathPredicate) {
        jsonpatcher$metaPatchPack.findResources(startingPath, allowedPathPredicate)
                .forEach((id, resource) -> map.computeIfAbsent(id, _ -> new ArrayList<>()).add(resource));
        map.keySet().removeIf(jsonpatcher$metaPatchPack::isDeleted);
        map.forEach((id, resources) -> resources.forEach(resource -> jsonpatcher$context.patchResource(id, resource)));
        return map;
    }

    @Override
    public MetapatchPackResources jsonpatcher$getMetaPatchPack() {
        return jsonpatcher$metaPatchPack;
    }
}
