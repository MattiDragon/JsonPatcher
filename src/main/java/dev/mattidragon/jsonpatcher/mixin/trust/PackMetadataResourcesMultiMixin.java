package dev.mattidragon.jsonpatcher.mixin.trust;

import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import dev.mattidragon.jsonpatcher.trust.TrustProvider;
import net.minecraft.server.packs.AbstractPackMetadataResources;
import net.minecraft.server.packs.FixedPathPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({AbstractPackMetadataResources.class, FixedPathPackResources.class})
public class PackMetadataResourcesMultiMixin implements TrustProvider {
    @Unique
    private final TrustLevel jsonpatcher$trustLevel = TrustProvider.TRUST_LEVEL_PASSTHROUGH.orElse(TrustLevel.UNTRUSTED);

    @Override
    public TrustLevel jsonpatcher$trustLevel() {
        return jsonpatcher$trustLevel;
    }
}
