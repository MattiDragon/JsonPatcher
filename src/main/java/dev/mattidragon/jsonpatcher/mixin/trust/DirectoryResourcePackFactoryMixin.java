package dev.mattidragon.jsonpatcher.mixin.trust;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.mattidragon.jsonpatcher.trust.MutableTrustProvider;
import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import net.minecraft.server.packs.PathPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PathPackResources.PathResourcesSupplier.class)
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
            method = {"openPrimary", "openFull"},
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/server/packs/PackLocationInfo;Ljava/nio/file/Path;)Lnet/minecraft/server/packs/PathPackResources;")
    )
    private PathPackResources injectTrust(PathPackResources original) {
        ((MutableTrustProvider)original).jsonpatcher$setTrustLevel(jsonpatcher$trustLevel);
        return original;
    }
}
