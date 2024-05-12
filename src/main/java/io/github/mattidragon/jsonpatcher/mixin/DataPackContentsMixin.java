package io.github.mattidragon.jsonpatcher.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.github.mattidragon.jsonpatcher.misc.ReloadDescription;
import io.github.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.DataPackContents;
import net.minecraft.server.command.CommandManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(DataPackContents.class)
public class DataPackContentsMixin {
    @Inject(method = "reload", at = @At("HEAD"))
    private static void setupContextForDataReload(ResourceManager manager,
                                                  CombinedDynamicRegistries<ServerDynamicRegistryType> dynamicRegistries,
                                                  FeatureSet enabledFeatures,
                                                  CommandManager.RegistrationEnvironment environment,
                                                  int functionPermissionLevel,
                                                  Executor prepareExecutor1,
                                                  Executor applyExecutor1,
                                                  CallbackInfoReturnable<CompletableFuture<DataPackContents>> cir,
                                                  @Local(argsOnly = true, ordinal = 0) LocalRef<Executor> prepareExecutor, 
                                                  @Local(argsOnly = true, ordinal = 1) LocalRef<Executor> applyExecutor) {
        var context = new PatchingContext(new ReloadDescription("datapacks", "data", error -> {}));

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
    
    @Inject(method = "reload", at = @At("TAIL")) 
    private static void tearDownContextForDataReload(ResourceManager manager,
                                                     CombinedDynamicRegistries<ServerDynamicRegistryType> dynamicRegistries,
                                                     FeatureSet enabledFeatures,
                                                     CommandManager.RegistrationEnvironment environment,
                                                     int functionPermissionLevel,
                                                     Executor prepareExecutor,
                                                     Executor applyExecutor,
                                                     CallbackInfoReturnable<CompletableFuture<DataPackContents>> cir) {
        PatchingContext.remove();
    }
}
