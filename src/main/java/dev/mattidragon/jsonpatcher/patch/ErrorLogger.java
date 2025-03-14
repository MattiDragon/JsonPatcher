package dev.mattidragon.jsonpatcher.patch;

import net.minecraft.text.Text;

import java.util.function.Consumer;

public interface ErrorLogger extends Consumer<Text> {
    ThreadLocal<ErrorLogger> CURRENT = ThreadLocal.withInitial(() -> error -> {});
}
