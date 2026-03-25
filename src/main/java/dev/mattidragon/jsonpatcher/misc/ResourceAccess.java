package dev.mattidragon.jsonpatcher.misc;

import net.minecraft.server.packs.resources.IoSupplier;

import java.io.InputStream;
import java.util.function.UnaryOperator;

public interface ResourceAccess {
    void jsonpatcher$disableKnowPack();
    void jsonpatcher$modifyStreamSupplier(UnaryOperator<IoSupplier<InputStream>> operator);
}
