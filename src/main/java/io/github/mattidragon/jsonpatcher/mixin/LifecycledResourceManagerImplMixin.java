package io.github.mattidragon.jsonpatcher.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.mattidragon.jsonpatcher.metapatch.MetapatchResourcePack;
import io.github.mattidragon.jsonpatcher.metapatch.MetapatchSingleResourceManager;
import io.github.mattidragon.jsonpatcher.misc.MetaPatchPackAccess;
import net.minecraft.resource.*;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Predicate;

@Mixin(LifecycledResourceManagerImpl.class)
public class LifecycledResourceManagerImplMixin implements MetaPatchPackAccess {
    @Unique
    private MetapatchResourcePack jsonpatcher$metaPatchPack;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Object;<init>()V", shift = At.Shift.AFTER, remap = false))
    private void init(ResourceType type, List<ResourcePack> packs, CallbackInfo ci) {
        jsonpatcher$metaPatchPack = new MetapatchResourcePack(type);
    }

    @ModifyReturnValue(method = "getAllNamespaces", at = @At("RETURN"))
    private Set<String> patchNamespaceSet(Set<String> value) {
        var set = new HashSet<>(value);
        set.addAll(jsonpatcher$metaPatchPack.getNamespaces(jsonpatcher$metaPatchPack.type));
        return set;
    }

    @SuppressWarnings("InvalidInjectorMethodSignature") // McDev can't find the variable for some reason
    @ModifyVariable(method = {"getResource", "getAllResources"}, at = @At("STORE"))
    private ResourceManager wrapManagerForHack(ResourceManager manager, Identifier id) {
        return new MetapatchSingleResourceManager(id, manager, jsonpatcher$metaPatchPack);
    }

    @ModifyVariable(method = "findResources", at = @At("TAIL"))
    private Map<Identifier, Resource> injectResourcesIntoFind(Map<Identifier, Resource> map, String startingPath, Predicate<Identifier> allowedPathPredicate) {
        map.putAll(jsonpatcher$metaPatchPack.findResources(startingPath, allowedPathPredicate));
        jsonpatcher$metaPatchPack.getDeletedFiles().forEach(map::remove);
        return map;
    }

    @ModifyVariable(method = "findAllResources", at = @At("TAIL"))
    private Map<Identifier, List<Resource>> injectResourcesIntoFindAll(Map<Identifier, List<Resource>> map, String startingPath, Predicate<Identifier> allowedPathPredicate) {
        jsonpatcher$metaPatchPack.findResources(startingPath, allowedPathPredicate)
                .forEach((id, resource) -> map.computeIfAbsent(id, i -> new ArrayList<>()).add(resource));
        jsonpatcher$metaPatchPack.getDeletedFiles().forEach(map::remove);
        return map;
    }

    @Override
    public MetapatchResourcePack jsonpatcher$getMetaPatchPack() {
        return jsonpatcher$metaPatchPack;
    }
}
