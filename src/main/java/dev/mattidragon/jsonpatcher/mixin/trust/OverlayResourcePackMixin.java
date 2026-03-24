package dev.mattidragon.jsonpatcher.mixin.trust;

import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import dev.mattidragon.jsonpatcher.trust.TrustProvider;
import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.PackResources;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CompositePackResources.class)
public class OverlayResourcePackMixin implements TrustProvider {
    @Shadow @Final private PackResources primaryPackResources;

    @Override
    public TrustLevel jsonpatcher$trustLevel() {
        return ((TrustProvider)primaryPackResources).jsonpatcher$trustLevel();
    }
}
