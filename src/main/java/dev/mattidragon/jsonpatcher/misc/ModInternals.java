package dev.mattidragon.jsonpatcher.misc;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public class ModInternals {
    private ModInternals() {
    }

    public static Object runWithFinally(Supplier<Object> main, Runnable last) {
        try {
            return main.get();
        } finally {
            last.run();
        }
    }
}
