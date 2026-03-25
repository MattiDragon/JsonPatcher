package dev.mattidragon.jsonpatcher.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(RegistryDataLoader.class)
public abstract class RegistryDataLoaderMixin {
    @WrapOperation(method = "load(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceProvider;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/RegistryDataLoader;load(Lnet/minecraft/resources/RegistryDataLoader$LoaderFactory;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private static CompletableFuture<RegistryAccess.Frozen> disablePatchingForNetworkRegistries(
            @Coerce Object loaderFactory,
            List<HolderLookup.RegistryLookup<?>> contextRegistries,
            List<RegistryDataLoader.RegistryData<?>> registriesToLoad,
            Executor executor,
            Operation<CompletableFuture<RegistryAccess.Frozen>> original) {
        try (var _ = PatchingContext.disablePatching()) {
            return original.call(loaderFactory, contextRegistries, registriesToLoad, executor);
        }
    }
}
