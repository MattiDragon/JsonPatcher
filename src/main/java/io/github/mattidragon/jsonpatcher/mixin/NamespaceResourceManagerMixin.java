package io.github.mattidragon.jsonpatcher.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.resource.NamespaceResourceManager;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NamespaceResourceManager.class)
public class NamespaceResourceManagerMixin {
    @ModifyExpressionValue(method = "createResource", at = @At(value = "NEW", target = "(Lnet/minecraft/resource/ResourcePack;Lnet/minecraft/resource/InputSupplier;Lnet/minecraft/resource/InputSupplier;)Lnet/minecraft/resource/Resource;"))
    private static Resource patchResource(Resource resource, @Local(argsOnly = true) Identifier id) {
        return PatchingContext.patchResource(id, resource);
    }
    
    @ModifyExpressionValue(method = "getAllResources", at = @At(value = "NEW", target = "(Lnet/minecraft/resource/ResourcePack;Lnet/minecraft/resource/InputSupplier;Lnet/minecraft/resource/InputSupplier;)Lnet/minecraft/resource/Resource;"))
    private Resource patchSpecialResource(Resource resource, Identifier id) {
        return PatchingContext.patchResource(id, resource);
    }
}
