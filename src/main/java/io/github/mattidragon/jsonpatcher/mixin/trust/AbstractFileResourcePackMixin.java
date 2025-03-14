package io.github.mattidragon.jsonpatcher.mixin.trust;

import io.github.mattidragon.jsonpatcher.trust.MutableTrustProvider;
import io.github.mattidragon.jsonpatcher.trust.TrustLevel;
import net.minecraft.resource.AbstractFileResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractFileResourcePack.class)
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
