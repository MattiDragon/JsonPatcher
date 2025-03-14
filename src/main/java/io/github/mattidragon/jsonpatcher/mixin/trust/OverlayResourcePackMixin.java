package io.github.mattidragon.jsonpatcher.mixin.trust;

import io.github.mattidragon.jsonpatcher.trust.TrustLevel;
import io.github.mattidragon.jsonpatcher.trust.TrustProvidingPack;
import net.minecraft.resource.OverlayResourcePack;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(OverlayResourcePack.class)
public class OverlayResourcePackMixin implements ResourcePackMixin {
    @Shadow @Final private ResourcePack base;

    @Override
    public TrustLevel jsonpatcher$trustLevel() {
        return ((TrustProvidingPack)base).jsonpatcher$trustLevel();
    }
}
