package io.github.mattidragon.jsonpatcher.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.mattidragon.jsonpatcher.patch.PatchingContext;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.NamespaceResourceManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.InputStream;

@Mixin(NamespaceResourceManager.class)
public class NamespaceResourceManagerMixin {
    @ModifyVariable(method = "createResource", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static InputSupplier<InputStream> patchNormalDataStream(InputSupplier<InputStream> value, @Local Identifier id) {
        return PatchingContext.patchInputStream(id, value);
    }

    @ModifyVariable(method = "getAllResources", at = @At(value = "NEW", target = "(Lnet/minecraft/resource/ResourcePack;Lnet/minecraft/resource/InputSupplier;Lnet/minecraft/resource/InputSupplier;)Lnet/minecraft/resource/Resource;"), ordinal = 0)
    private InputSupplier<InputStream> patchSpecialDataStream(InputSupplier<InputStream> value, Identifier id) {
        return PatchingContext.patchInputStream(id, value);
    }
}
