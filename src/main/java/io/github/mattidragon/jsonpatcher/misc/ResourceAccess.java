package io.github.mattidragon.jsonpatcher.misc;

import net.minecraft.resource.InputSupplier;

import java.io.InputStream;
import java.util.function.UnaryOperator;

public interface ResourceAccess {
    void jsonpatcher$disableKnowPack();
    void jsonpatcher$modifyInputStreamSupplier(UnaryOperator<InputSupplier<InputStream>> operator);
}
