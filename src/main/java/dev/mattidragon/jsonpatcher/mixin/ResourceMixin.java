package dev.mattidragon.jsonpatcher.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.mattidragon.jsonpatcher.misc.ResourceAccess;
import net.minecraft.registry.VersionedIdentifier;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.Resource;
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
    private InputSupplier<InputStream> inputSupplier;

    @Override
    public void jsonpatcher$disableKnowPack() {
        disableKnowPack = true;
    }

    @Override
    public void jsonpatcher$modifyInputStreamSupplier(UnaryOperator<InputSupplier<InputStream>> operator) {
        inputSupplier = operator.apply(inputSupplier);
    }

    @ModifyReturnValue(method = "getKnownPackInfo", at = @At("RETURN"))
    private Optional<VersionedIdentifier> disableKnownPackInfoForPatchedResources(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<VersionedIdentifier> original) {
        if (disableKnowPack) {
            return Optional.empty();
        } else {
            return original;
        }
    }
}
