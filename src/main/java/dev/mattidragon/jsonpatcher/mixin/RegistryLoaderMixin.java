package dev.mattidragon.jsonpatcher.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.registry.*;
import net.minecraft.resource.ResourceFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.util.List;
import java.util.Map;

@Mixin(RegistryLoader.class)
public abstract class RegistryLoaderMixin {
    @WrapOperation(method = "loadFromNetwork(Ljava/util/Map;Lnet/minecraft/resource/ResourceFactory;Lnet/minecraft/registry/DynamicRegistryManager;Ljava/util/List;)Lnet/minecraft/registry/DynamicRegistryManager$Immutable;", 
            at = @At(value = "INVOKE", target = "Lnet/minecraft/registry/RegistryLoader;load(Lnet/minecraft/registry/RegistryLoader$RegistryLoadable;Lnet/minecraft/registry/DynamicRegistryManager;Ljava/util/List;)Lnet/minecraft/registry/DynamicRegistryManager$Immutable;"))
    private static DynamicRegistryManager.Immutable disablePatchingForNetworkRegistries(@Coerce Object loadable,
                                                                                        DynamicRegistryManager baseRegistryManager,
                                                                                        List<RegistryLoader.Entry<?>> entries,
                                                                                        Operation<DynamicRegistryManager.Immutable> original,
                                                                                        Map<RegistryKey<? extends Registry<?>>, List<SerializableRegistries.SerializedRegistryEntry>> data,
                                                                                        ResourceFactory factory,
                                                                                        DynamicRegistryManager registryManager,
                                                                                        List<RegistryLoader.Entry<?>> entries2) {
        try (var __ = PatchingContext.disablePatching()) {
            return original.call(loadable, baseRegistryManager, entries);
        }
    }
}
