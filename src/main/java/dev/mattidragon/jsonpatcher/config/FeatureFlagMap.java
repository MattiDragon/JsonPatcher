package dev.mattidragon.jsonpatcher.config;

import com.mojang.serialization.Codec;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class FeatureFlagMap {
    public static final Codec<FeatureFlagMap> CODEC = Codec.unboundedMap(FeatureFlag.CODEC, Codec.BOOL)
            .xmap(FeatureFlagMap::new, map -> map.values);

    private final Map<FeatureFlag, Boolean> values;

    FeatureFlagMap(Map<FeatureFlag, Boolean> values) {
        this.values = new HashMap<>(values);
        for (var value : FeatureFlag.BuiltIn.values()) {
            this.values.putIfAbsent(value, false);
        }
    }

    public boolean isEnabled(FeatureFlag flag) {
        return values.getOrDefault(flag, false);
    }

    public Collection<FeatureFlag> enabledFlags() {
        return values.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toList();
    }
}
