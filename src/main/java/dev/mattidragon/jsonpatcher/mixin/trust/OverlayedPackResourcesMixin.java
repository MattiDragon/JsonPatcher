package dev.mattidragon.jsonpatcher.mixin.trust;

import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import dev.mattidragon.jsonpatcher.trust.TrustProvider;
import net.minecraft.server.packs.OverlayedPackResources;
import net.minecraft.server.packs.PackMetadataResources;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(OverlayedPackResources.class)
public class OverlayedPackResourcesMixin implements TrustProvider {
    @Shadow @Final private PackMetadataResources primaryPackMetadataResources;

    @Override
    public TrustLevel jsonpatcher$trustLevel() {
        return ((TrustProvider)primaryPackMetadataResources).jsonpatcher$trustLevel();
    }
}
