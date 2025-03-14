package io.github.mattidragon.jsonpatcher.mixin.trust;

import io.github.mattidragon.jsonpatcher.trust.TrustLevel;
import io.github.mattidragon.jsonpatcher.trust.TrustProvider;
import net.minecraft.resource.OverlayResourcePack;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(OverlayResourcePack.class)
public class OverlayResourcePackMixin implements TrustProvider {
    @Shadow @Final private ResourcePack base;

    @Override
    public TrustLevel jsonpatcher$trustLevel() {
        return ((TrustProvider)base).jsonpatcher$trustLevel();
    }
}
