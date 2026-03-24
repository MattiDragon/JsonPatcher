package dev.mattidragon.jsonpatcher.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.util.List;
import java.util.Map;

@Mixin(RegistryDataLoader.class)
public abstract class RegistryLoaderMixin {
    @WrapOperation(method = "load(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;", 
            at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/RegistryDataLoader;load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;"))
    private static RegistryAccess.Frozen disablePatchingForNetworkRegistries(@Coerce Object loadable,
                                                                                        RegistryAccess baseRegistryManager,
                                                                                        List<RegistryDataLoader.RegistryData<?>> entries,
                                                                                        Operation<RegistryAccess.Frozen> original,
                                                                                        Map<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> data,
                                                                                        ResourceProvider factory,
                                                                                        RegistryAccess registryManager,
                                                                                        List<RegistryDataLoader.RegistryData<?>> entries2) {
        try (var __ = PatchingContext.disablePatching()) {
            return original.call(loadable, baseRegistryManager, entries);
        }
    }
}
