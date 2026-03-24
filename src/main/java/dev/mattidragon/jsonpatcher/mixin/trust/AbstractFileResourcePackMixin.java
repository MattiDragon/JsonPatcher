package dev.mattidragon.jsonpatcher.mixin.trust;

import dev.mattidragon.jsonpatcher.trust.MutableTrustProvider;
import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import net.minecraft.server.packs.AbstractPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractPackResources.class)
public class AbstractFileResourcePackMixin implements MutableTrustProvider {
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
}
