package dev.mattidragon.jsonpatcher.misc;

import java.io.InputStream;
import java.util.function.UnaryOperator;
import net.minecraft.server.packs.resources.IoSupplier;

public interface ResourceAccess {
    void jsonpatcher$disableKnowPack();
    void jsonpatcher$modifyStreamSupplier(UnaryOperator<IoSupplier<InputStream>> operator);
}
