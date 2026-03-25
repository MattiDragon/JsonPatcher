package dev.mattidragon.jsonpatcher.mixin.trust;

import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import dev.mattidragon.jsonpatcher.trust.TrustProvider;
import net.fabricmc.fabric.api.resource.v1.pack.ModPackResources;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ModPackResources.class)
public interface ModPackResourcesMixin extends TrustProvider {
    @Override
    default TrustLevel jsonpatcher$trustLevel() {
        return TrustLevel.MOD;
    }
}
