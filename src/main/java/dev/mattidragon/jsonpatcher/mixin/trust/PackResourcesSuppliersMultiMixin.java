package dev.mattidragon.jsonpatcher.mixin.trust;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import dev.mattidragon.jsonpatcher.trust.TrustProvider;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.stream.Stream;

@Mixin({FilePackResources.FileResourcesSupplier.class, PathPackResources.PathResourcesSupplier.class})
public class PackResourcesSuppliersMultiMixin implements TrustProvider {
    @Unique
    private final TrustLevel jsonpatcher$trustLevel = TrustProvider.TRUST_LEVEL_PASSTHROUGH.orElse(TrustLevel.UNTRUSTED);

    @Override
    public TrustLevel jsonpatcher$trustLevel() {
        return jsonpatcher$trustLevel;
    }

    @WrapMethod(method = "openMetadata")
    private PackMetadataResources injectTrust(PackLocationInfo location, Operation<PackMetadataResources> original) {
        return ScopedValue.where(TrustProvider.TRUST_LEVEL_PASSTHROUGH, jsonpatcher$trustLevel)
                .call(() -> original.call(location));
    }

    @WrapMethod(method = "openResources")
    private Stream<PackResources> injectTrust(PackLocationInfo location, Pack.Metadata metadata, Operation<Stream<PackResources>> original) {
        return ScopedValue.where(TrustProvider.TRUST_LEVEL_PASSTHROUGH, jsonpatcher$trustLevel)
                .call(() -> original.call(location, metadata));
    }
}
