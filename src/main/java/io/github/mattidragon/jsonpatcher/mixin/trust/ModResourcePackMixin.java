package io.github.mattidragon.jsonpatcher.mixin.trust;

import io.github.mattidragon.jsonpatcher.trust.TrustLevel;
import io.github.mattidragon.jsonpatcher.trust.TrustProvider;
import net.fabricmc.fabric.api.resource.ModResourcePack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ModResourcePack.class)
public interface ModResourcePackMixin extends TrustProvider {
    @Override
    default TrustLevel jsonpatcher$trustLevel() {
        return TrustLevel.MOD;
    }
}
