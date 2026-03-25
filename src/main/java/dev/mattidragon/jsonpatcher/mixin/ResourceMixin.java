package dev.mattidragon.jsonpatcher.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.mattidragon.jsonpatcher.misc.ResourceAccess;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import java.io.InputStream;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Mixin(Resource.class)
public class ResourceMixin implements ResourceAccess {
    @Unique
    private boolean disableKnowPack = false;
    
    @Final
    @Mutable
    @Shadow
    private IoSupplier<InputStream> streamSupplier;

    @Override
    public void jsonpatcher$disableKnowPack() {
        disableKnowPack = true;
    }

    @Override
    public void jsonpatcher$modifyStreamSupplier(UnaryOperator<IoSupplier<InputStream>> operator) {
        streamSupplier = operator.apply(streamSupplier);
    }

    @ModifyReturnValue(method = "knownPackInfo", at = @At("RETURN"))
    private Optional<KnownPack> disableKnownPackInfoForPatchedResources(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<KnownPack> original) {
        if (disableKnowPack) {
            return Optional.empty();
        } else {
            return original;
        }
    }
}
