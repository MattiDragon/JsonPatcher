package io.github.mattidragon.jsonpatcher.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.mattidragon.jsonpatcher.ReloadDescription;
import io.github.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.resource.LifecycledResourceManager;
import net.minecraft.server.SaveLoading;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(SaveLoading.class)
public class SaveLoadingMixin {
    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/SaveLoading;withRegistriesLoaded(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/registry/CombinedDynamicRegistries;Lnet/minecraft/registry/ServerDynamicRegistryType;Ljava/util/List;)Lnet/minecraft/registry/CombinedDynamicRegistries;"))
    private static <D, R> void setupContext(SaveLoading.ServerConfig serverConfig,
                                            SaveLoading.LoadContextSupplier<D> loadContextSupplier,
                                            SaveLoading.SaveApplierFactory<D, R> saveApplierFactory,
                                            Executor prepareExecutor,
                                            Executor applyExecutor,
                                            CallbackInfoReturnable<CompletableFuture<R>> cir,
                                            @Local LifecycledResourceManager lifecycledResourceManager) {
        var context = new PatchingContext(new ReloadDescription("dynamic registries", error -> {}));
        context.load(lifecycledResourceManager, prepareExecutor);
        PatchingContext.set(context);
    }

    @Inject(method = "load", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/registry/RegistryLoader;load(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/registry/DynamicRegistryManager;Ljava/util/List;)Lnet/minecraft/registry/DynamicRegistryManager$Immutable;"))
    private static <D, R> void removeContext(SaveLoading.ServerConfig serverConfig,
                                            SaveLoading.LoadContextSupplier<D> loadContextSupplier,
                                            SaveLoading.SaveApplierFactory<D, R> saveApplierFactory,
                                            Executor prepareExecutor,
                                            Executor applyExecutor,
                                            CallbackInfoReturnable<CompletableFuture<R>> cir) {
        PatchingContext.remove();
    }

    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/DataPackContents;reload(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/registry/DynamicRegistryManager$Immutable;Lnet/minecraft/resource/featuretoggle/FeatureSet;Lnet/minecraft/server/command/CommandManager$RegistrationEnvironment;ILjava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private static <D, R> void setDescriptionForReload(SaveLoading.ServerConfig serverConfig,
                                                             SaveLoading.LoadContextSupplier<D> loadContextSupplier,
                                                             SaveLoading.SaveApplierFactory<D, R> saveApplierFactory,
                                                             Executor prepareExecutor,
                                                             Executor applyExecutor,
                                                             CallbackInfoReturnable<CompletableFuture<R>> cir) {
        ReloadDescription.CURRENT.set(new ReloadDescription("datapacks (initial)", error -> {}));
    }
}
