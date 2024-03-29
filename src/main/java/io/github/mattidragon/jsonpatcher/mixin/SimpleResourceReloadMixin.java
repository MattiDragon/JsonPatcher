package io.github.mattidragon.jsonpatcher.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.github.mattidragon.jsonpatcher.misc.ReloadDescription;
import io.github.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReload;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.SimpleResourceReload;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(SimpleResourceReload.class)
public class SimpleResourceReloadMixin<S> {

    @Inject(method = "start", at = @At("HEAD"))
    private static void setupPatching(ResourceManager manager,
                                      List<ResourceReloader> reloaders,
                                      Executor prepareExecutor1,
                                      Executor applyExecutor1,
                                      CompletableFuture<Unit> initialStage1,
                                      boolean profiled,
                                      CallbackInfoReturnable<ResourceReload> cir,
                                      @Local(argsOnly = true, ordinal = 0) LocalRef<Executor> prepareExecutor,
                                      @Local(argsOnly = true, ordinal = 1) LocalRef<Executor> applyExecutor) {
        var context = new PatchingContext(ReloadDescription.pop());

        // Setup context for constructor because some reloaders get resources on the reload thread
        PatchingContext.set(context);

        // Patches have to be loaded here instead of in the initial stage because some reloaders read resources on the main thread (font manager)
        context.load(manager, prepareExecutor1);

        // Patch the prepare executor to apply patches
        prepareExecutor.set(command -> prepareExecutor1.execute(() -> {
            PatchingContext.set(context);
            command.run();
            PatchingContext.remove();
        }));

        // Patch the apply executor to apply patches. This is done for safety, some mod might decide to load resources here
        applyExecutor.set(command -> applyExecutor1.execute(() -> {
            PatchingContext.set(context);
            command.run();
            PatchingContext.remove();
        }));
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void removeContextFromThread(Executor prepareExecutor1,
                                         Executor applyExecutor1,
                                         ResourceManager manager,
                                         List<ResourceReloader> reloaders,
                                         SimpleResourceReload.Factory<S> factory,
                                         CompletableFuture<Unit> initialStage1,
                                         CallbackInfo ci) {
        PatchingContext.remove();
    }
}
