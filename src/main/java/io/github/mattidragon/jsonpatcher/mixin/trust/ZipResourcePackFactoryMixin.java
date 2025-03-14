package io.github.mattidragon.jsonpatcher.mixin.trust;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.mattidragon.jsonpatcher.trust.MutableTrustProvider;
import io.github.mattidragon.jsonpatcher.trust.TrustLevel;
import net.minecraft.resource.ZipResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ZipResourcePack.ZipBackedFactory.class)
public class ZipResourcePackFactoryMixin implements MutableTrustProvider {
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
                    target = "(Lnet/minecraft/resource/ResourcePackInfo;Lnet/minecraft/resource/ZipResourcePack$ZipFileWrapper;Ljava/lang/String;)Lnet/minecraft/resource/ZipResourcePack;")
    )
    private ZipResourcePack injectTrust(ZipResourcePack original) {
        ((MutableTrustProvider)original).jsonpatcher$setTrustLevel(jsonpatcher$trustLevel);
        return original;
    }
}
