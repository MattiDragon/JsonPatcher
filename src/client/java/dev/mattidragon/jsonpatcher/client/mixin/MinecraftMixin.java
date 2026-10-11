package dev.mattidragon.jsonpatcher.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import dev.mattidragon.jsonpatcher.trust.TrustProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Path;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @WrapOperation(method = "<init>", at = @At(value = "NEW", target = "(Ljava/nio/file/Path;Lnet/minecraft/server/packs/PackType;Lnet/minecraft/server/packs/repository/PackSource;Lnet/minecraft/world/level/validation/DirectoryValidator;)Lnet/minecraft/server/packs/repository/FolderRepositorySource;"))
    private FolderRepositorySource setResourcePackTrust(Path folder, PackType packType, PackSource packSource, DirectoryValidator validator, Operation<FolderRepositorySource> original) {
        return ScopedValue.where(TrustProvider.TRUST_LEVEL_PASSTHROUGH, TrustLevel.LOCAL)
                .call(() -> original.call(folder, packType, packSource, validator));
    }
}
