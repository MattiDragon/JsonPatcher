package io.github.mattidragon.jsonpatcher;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A description of a reload. Contains meta info about a reload. Assigned using various mixins.
 *
 * @param name The short name describing the reload.
 * @param dumpPath The location where files of this reload will be dumped. If null no dump will be created.
 * @param errorConsumer A consumer that will be called with an error message if the reload fails. Errors will also be logged.
 */
public record ReloadDescription(String name, @Nullable String dumpPath, Consumer<Text> errorConsumer) {
    public static final ReloadDescription UNKNOWN = new ReloadDescription("unknown", null, error -> {});

    public static final ThreadLocal<ReloadDescription> CURRENT = ThreadLocal.withInitial(() -> UNKNOWN);

    public static ReloadDescription pop() {
        var current = CURRENT.get();
        CURRENT.remove();
        return current;
    }
}
