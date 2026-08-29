package dev.mattidragon.jsonpatcher.config;

import com.mojang.serialization.Codec;
import dev.mattidragon.jsonpatcher.JsonPatcher;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public sealed interface FeatureFlag {
    Codec<FeatureFlag> CODEC = Identifier.CODEC.xmap(
            FeatureFlag::byId,
            FeatureFlag::id
    );

    static FeatureFlag byId(Identifier id) {
        var flag = BuiltIn.BY_ID.get(id);
        if (flag != null) return flag;
        return new Custom(id);
    }

    Identifier id();

    enum BuiltIn implements FeatureFlag {
        RECIPEPATCHER;

        public static final Map<Identifier, BuiltIn> BY_ID = Arrays.stream(values())
                .collect(Collectors.toMap(BuiltIn::id, Function.identity()));

        private final Identifier id;

        BuiltIn() {
            id = JsonPatcher.id(name().toLowerCase(Locale.ROOT));
        }

        @Override
        public Identifier id() {
            return id;
        }
    }

    record Custom(Identifier id) implements FeatureFlag {
        public Custom {
            for (var value : BuiltIn.values()) {
                if (value.id().equals(id)) {
                    throw new IllegalArgumentException("Custom feature flag id cannot be same as built-in feature flag id: " + id);
                }
            }
        }
    }
}
