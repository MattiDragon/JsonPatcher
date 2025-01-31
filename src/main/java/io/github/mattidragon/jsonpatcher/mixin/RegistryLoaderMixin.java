package io.github.mattidragon.jsonpatcher.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.registry.*;
import net.minecraft.resource.ResourceFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.util.List;
import java.util.Map;

@Mixin(RegistryLoader.class)
public abstract class RegistryLoaderMixin {
    @WrapOperation(method = "loadFromNetwork(Ljava/util/Map;Lnet/minecraft/resource/ResourceFactory;Ljava/util/List;Ljava/util/List;)Lnet/minecraft/registry/DynamicRegistryManager$Immutable;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/registry/RegistryLoader;load(Lnet/minecraft/registry/RegistryLoader$RegistryLoadable;Ljava/util/List;Ljava/util/List;)Lnet/minecraft/registry/DynamicRegistryManager$Immutable;"))
    private static DynamicRegistryManager.Immutable disablePatchingForNetworkRegistries(@Coerce Object loadable,
                                                                                        List<RegistryWrapper.Impl<?>> registries,
                                                                                        List<RegistryLoader.Entry<?>> entries,
                                                                                        Operation<DynamicRegistryManager.Immutable> original) {
        try (var __ = PatchingContext.disablePatching()) {
            return original.call(loadable, registries, entries);
        }
    }
}
