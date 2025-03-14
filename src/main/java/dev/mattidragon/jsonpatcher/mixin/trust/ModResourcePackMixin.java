package dev.mattidragon.jsonpatcher.mixin.trust;

import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import dev.mattidragon.jsonpatcher.trust.TrustProvider;
import net.fabricmc.fabric.api.resource.ModResourcePack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ModResourcePack.class)
public interface ModResourcePackMixin extends TrustProvider {
    @Override
    default TrustLevel jsonpatcher$trustLevel() {
        return TrustLevel.MOD;
    }
}
