package dev.mattidragon.jsonpatcher.patch;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

public interface ErrorLogger extends Consumer<Component> {
    ThreadLocal<ErrorLogger> CURRENT = ThreadLocal.withInitial(() -> error -> {});
}
