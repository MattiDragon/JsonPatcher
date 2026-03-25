package dev.mattidragon.jsonpatcher.patch;

import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public interface ErrorLogger extends Consumer<Component> {
    ThreadLocal<ErrorLogger> CURRENT = ThreadLocal.withInitial(() -> _ -> {});
}
