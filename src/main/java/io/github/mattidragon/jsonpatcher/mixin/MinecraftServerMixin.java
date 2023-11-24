package io.github.mattidragon.jsonpatcher.mixin;

import com.google.common.collect.ImmutableList;
import io.github.mattidragon.jsonpatcher.ReloadDescription;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.server.DataPackContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletionStage;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow public abstract PlayerManager getPlayerManager();

    @Inject(method = "method_29437", at = @At("HEAD"))
    private void setReloadDescription(DynamicRegistryManager.Immutable immutable, ImmutableList<ResourcePack> resourcePacks, CallbackInfoReturnable<CompletionStage<DataPackContents>> cir) {
        ReloadDescription.CURRENT.set(new ReloadDescription("datapacks", "data", error -> getPlayerManager().broadcast(error, false)));
    }
}
