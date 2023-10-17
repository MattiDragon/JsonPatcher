package io.github.mattidragon.jsonpatcher.lang.parse;

import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

import java.util.LinkedHashMap;
import java.util.Map;

public class PatchMetadata {
    private final Map<String, Value> values = new LinkedHashMap<>();

    public void add(String key, Parser parser) {
        var value = new JsonParser(parser).parse();
        values.put(key, value);
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public Value get(String key) {
        return values.get(key);
    }

    public Value.ObjectValue getObject(String key) {
        if (values.get(key) instanceof Value.ObjectValue object) return object;
        throw new IllegalStateException("Expected object for meta key '%s', got '%s'".formatted(key, values.get(key)));
    }

    public Value.ArrayValue getArray(String key) {
        if (values.get(key) instanceof Value.ArrayValue array) return array;
        throw new IllegalStateException("Expected array for meta key '%s', got '%s'".formatted(key, values.get(key)));
    }

    public String getString(String key) {
        if (values.get(key) instanceof Value.StringValue string) return string.value();
        throw new IllegalStateException("Expected string for meta key '%s', got '%s'".formatted(key, values.get(key)));
    }

    public double getNumber(String key) {
        if (values.get(key) instanceof Value.NumberValue number) return number.value();
        throw new IllegalStateException("Expected number for meta key '%s', got '%s'".formatted(key, values.get(key)));
    }

    public boolean getBoolean(String key) {
        if (values.get(key) instanceof Value.BooleanValue bool) return bool.value();
        throw new IllegalStateException("Expected boolean for meta key '%s', got '%s'".formatted(key, values.get(key)));
    }
}
