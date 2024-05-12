package io.github.mattidragon.jsonpatcher.client.mixin;

import io.github.mattidragon.jsonpatcher.misc.ReloadDescription;
import io.github.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.LifecycledResourceManager;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableResourceManagerImpl.class)
public class ReloadableResourceManagerImplMixin {
    @Shadow private LifecycledResourceManager activeManager;

    @ModifyArgs(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/SimpleResourceReload;start(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Z)Lnet/minecraft/resource/ResourceReload;"))
    private void setupContextForAssetReload(Args args) {
        var context = new PatchingContext(new ReloadDescription("resourcepacks", "assets", error -> {
            var player = MinecraftClient.getInstance().player;
            if (player != null) {
                player.sendMessage(error, false);
            }
        }));

        // Setup context for constructor because some reloaders get resources on the reload thread
        PatchingContext.set(context);

        var prepareExecutor = args.<Executor>get(2);
        var applyExecutor = args.<Executor>get(3);
        
        // Patches have to be loaded here instead of in the initial stage because some reloaders read resources on the main thread (font manager)
        context.load(activeManager, prepareExecutor);

        // Patch the prepare executor to apply patches
        args.<Executor>set(2, command -> prepareExecutor.execute(() -> {
            PatchingContext.set(context);
            command.run();
            PatchingContext.remove();
        }));

        // Patch the apply executor to apply patches. This is done for safety, some mod might decide to load resources here
        args.<Executor>set(3, command -> applyExecutor.execute(() -> {
            PatchingContext.set(context);
            command.run();
            PatchingContext.remove();
        }));
    }

    @Inject(method = "reload", at = @At("TAIL"))
    private void tearDownContextForAssetReload(Executor prepareExecutor,
                                               Executor applyExecutor,
                                               CompletableFuture<Unit> initialStage,
                                               List<ResourcePack> packs,
                                               CallbackInfoReturnable<ResourceReload> cir) {
        PatchingContext.remove();
    }
}
