package io.github.mattidragon.jsonpatcher.client.mixin;

import io.github.mattidragon.jsonpatcher.misc.ReloadDescription;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ReloadableResourceManagerImpl;reload(Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Ljava/util/List;)Lnet/minecraft/resource/ResourceReload;"))
    private void setReloadDescriptionForInitialReload(RunArgs args, CallbackInfo ci) {
        ReloadDescription.CURRENT.set(new ReloadDescription("resourcepacks", "assets", error -> {}));
    }

    @Inject(method = "reloadResources(ZLnet/minecraft/client/MinecraftClient$LoadingContext;)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
    private void setReloadDescription(boolean force, @Coerce Object loadingContext, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        ReloadDescription.CURRENT.set(new ReloadDescription("resourcepacks", "assets", error -> {
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(error, false);
            }
        }));
    }
}
