package dev.mattidragon.jsonpatcher.mixin.trust;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.mattidragon.jsonpatcher.trust.MutableTrustProvider;
import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import net.minecraft.server.packs.FilePackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FilePackResources.FileResourcesSupplier.class)
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
            method = {"openPrimary", "openFull"},
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/server/packs/PackLocationInfo;Lnet/minecraft/server/packs/FilePackResources$SharedZipFileAccess;Ljava/lang/String;)Lnet/minecraft/server/packs/FilePackResources;")
    )
    private FilePackResources injectTrust(FilePackResources original) {
        ((MutableTrustProvider)original).jsonpatcher$setTrustLevel(jsonpatcher$trustLevel);
        return original;
    }
}
