package dev.mattidragon.jsonpatcher.context;

import dev.mattidragon.jsonpatcher.config.Config;
import dev.mattidragon.jsonpatcher.config.FeatureFlag;
import dev.mattidragon.jsonpatcher.lang.runtime.value.PatchFunction;
import dev.mattidragon.jsonpatcher.lang.runtime.value.Value;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

// TODO: actually implement the global
public class ProgramContext {
    public static final Value.ObjectValue OBJECT;

    private static final ThreadLocal<Deque<String>> ROLES = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<@Nullable String> TARGET = new ThreadLocal<>();

    static {
        var loadedMods = new HashMap<String, Value>();
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            var metadata = mod.getMetadata();
            loadedMods.put(metadata.getId(), new Value.ObjectValue(Map.of(
                    "id", new Value.StringValue(metadata.getId()),
                    "name", new Value.StringValue(metadata.getName()),
                    "version", new Value.StringValue(metadata.getVersion().toString()),
                    "description", new Value.StringValue(metadata.getDescription())
            ), true));
        }

        var featureFlags = Config.MANAGER.get().featureFlags().enabledFlags()
                .stream()
                .map(FeatureFlag::id)
                .map(Identifier::toString)
                .<Value>map(Value.StringValue::new)
                .toList();

        PatchFunction.BuiltInPatchFunction role = (_, args) -> {
            if (!args.isEmpty()) {
                throw new IllegalArgumentException("role function does not take any arguments");
            }
            var roles = ROLES.get();
            return roles.isEmpty() ? Value.NullValue.NULL : new Value.StringValue(roles.peek());
        };

        PatchFunction.BuiltInPatchFunction target = (_, args) -> {
            if (!args.isEmpty()) {
                throw new IllegalArgumentException("target function does not take any arguments");
            }
            var targetString = TARGET.get();
            return targetString == null ? Value.NullValue.NULL : new Value.StringValue(targetString);
        };

        OBJECT = new Value.ObjectValue(Map.of(
                "loadedMods", new Value.ObjectValue(loadedMods, true),
                "role", new Value.FunctionValue(role),
                "target", new Value.FunctionValue(target),
                "enabledFeatureFlags", new Value.ArrayValue(featureFlags, true)
        ), true);
    }

    public static sealed class RoleContext implements AutoCloseable {
        public RoleContext(String role) {
            ROLES.get().push(role);
        }

        @Override
        public void close() {
            ROLES.get().pop();
        }
    }

    public static final class TargetContext extends RoleContext {
        public TargetContext(String role, String target) {
            super(role);
            if (TARGET.get() != null) {
                throw new IllegalStateException("Cannot set target when another target is already set");
            }
            TARGET.set(target);
        }

        @Override
        public void close() {
            super.close();
            TARGET.remove();
        }
    }
}
