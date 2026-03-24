package dev.mattidragon.jsonpatcher.misc;

import dev.mattidragon.jsonpatcher.lang.runtime.value.Value;

@SuppressWarnings("unused")
public class ModInternals {
    private ModInternals() {
    }

    public static Value runWithFinally(ValueSupplier main, Runnable last) {
        try {
            return main.get();
        } finally {
            last.run();
        }
    }

    public interface ValueSupplier {
        Value get();
    }
}
