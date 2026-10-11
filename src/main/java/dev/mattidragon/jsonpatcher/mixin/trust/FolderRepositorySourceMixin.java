package dev.mattidragon.jsonpatcher.mixin.trust;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import dev.mattidragon.jsonpatcher.trust.TrustProvider;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Consumer;

@Mixin(FolderRepositorySource.class)
public class FolderRepositorySourceMixin {
    @Unique
    private final TrustLevel jsonpatcher$trustLevel = TrustProvider.TRUST_LEVEL_PASSTHROUGH.orElse(TrustLevel.UNTRUSTED);

    @WrapMethod(method = "loadPacks")
    private void injectTrust(Consumer<Pack> result, Operation<Void> original) {
        ScopedValue.where(TrustProvider.TRUST_LEVEL_PASSTHROUGH, jsonpatcher$trustLevel)
                .run(() -> original.call(result));
    }
}
