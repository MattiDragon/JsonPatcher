package dev.mattidragon.jsonpatcher.mixin.trust;

import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import dev.mattidragon.jsonpatcher.trust.TrustProvider;
import net.minecraft.server.packs.PackResources;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PackResources.class)
public interface ResourcePackMixin extends TrustProvider {
    @Override
    default TrustLevel jsonpatcher$trustLevel() {
        return TrustLevel.UNTRUSTED;
    }
}
