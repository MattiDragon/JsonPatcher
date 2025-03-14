package io.github.mattidragon.jsonpatcher.mixin.trust;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.mattidragon.jsonpatcher.trust.MutableTrustProvider;
import io.github.mattidragon.jsonpatcher.trust.TrustLevel;
import net.minecraft.resource.DirectoryResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DirectoryResourcePack.DirectoryBackedFactory.class)
public class DirectoryResourcePackFactoryMixin implements MutableTrustProvider {
    @Unique
    private TrustLevel jsonpatcher$trustLevel = TrustLevel.UNTRUSTED;

    @Override
    public TrustLevel jsonpatcher$trustLevel() {
        return jsonpatcher$trustLevel;
    }

    @Override
    public void jsonpatcher$setTrustLevel(TrustLevel trustLevel) {
        jsonpatcher$trustLevel = trustLevel;
    }

    @ModifyExpressionValue(
            method = {"open", "openWithOverlays"},
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/resource/ResourcePackInfo;Ljava/nio/file/Path;)Lnet/minecraft/resource/DirectoryResourcePack;")
    )
    private DirectoryResourcePack injectTrust(DirectoryResourcePack original) {
        ((MutableTrustProvider)original).jsonpatcher$setTrustLevel(jsonpatcher$trustLevel);
        return original;
    }
}
