package io.github.mattidragon.jsonpatcher;

import com.google.gson.*;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class GsonConverter {
    private static final Set<Value> TO_GSON_RECURSION_TRACKER = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<JsonElement> FROM_GSON_RECURSION_TRACKER = Collections.newSetFromMap(new WeakHashMap<>());

    private GsonConverter() {
    }

    public static JsonElement toGson(Value value) {
        try {
            if (!TO_GSON_RECURSION_TRACKER.add(value)) {
                throw new IllegalStateException("recursive value tree");
            }
            if (value instanceof Value.ObjectValue objectValue) return toGson(objectValue);
            if (value instanceof Value.ArrayValue arrayValue) return toGson(arrayValue);
            if (value instanceof Value.NumberValue numberValue) return new JsonPrimitive(numberValue.value());
            if (value instanceof Value.StringValue stringValue) return new JsonPrimitive(stringValue.value());
            if (value instanceof Value.BooleanValue booleanValue) return new JsonPrimitive(booleanValue.value());
            if (value instanceof Value.NullValue) return JsonNull.INSTANCE;
            throw new IllegalStateException("Can't convert %s to gson".formatted(value));
        } finally {
            TO_GSON_RECURSION_TRACKER.remove(value);
        }
    }

    public static JsonObject toGson(Value.ObjectValue value) {
        JsonObject object = new JsonObject();
        for (var entry : value.value().entrySet()) {
            object.add(entry.getKey(), toGson(entry.getValue()));
        }
        return object;
    }

    public static JsonArray toGson(Value.ArrayValue value) {
        JsonArray array = new JsonArray();
        for (var entry : value.value()) {
            array.add(toGson(entry));
        }
        return array;
    }

    public static Value fromGson(JsonElement json) {
        try {
            if (!FROM_GSON_RECURSION_TRACKER.add(json)) {
                throw new IllegalStateException("recursive gson json tree");
            }
            if (json instanceof JsonObject jsonObject) return fromGson(jsonObject);
            if (json instanceof JsonArray jsonArray) return fromGson(jsonArray);
            if (json instanceof JsonPrimitive primitive) {
                if (primitive.isBoolean()) return Value.BooleanValue.of(primitive.getAsBoolean());
                if (primitive.isNumber()) return new Value.NumberValue(primitive.getAsNumber().doubleValue());
                if (primitive.isString()) return new Value.StringValue(primitive.getAsString());
            }
            if (json instanceof JsonNull) return Value.NullValue.NULL;
            throw new IllegalStateException("Can't convert %s to value".formatted(json));
        } finally {
            FROM_GSON_RECURSION_TRACKER.remove(json);
        }
    }

    public static Value.ObjectValue fromGson(JsonObject object) {
        Value.ObjectValue value = new Value.ObjectValue();
        for (var entry : object.entrySet()) {
            value.value().put(entry.getKey(), fromGson(entry.getValue()));
        }
        return value;
    }

    public static Value.ArrayValue fromGson(JsonArray array) {
        Value.ArrayValue value = new Value.ArrayValue();
        for (var entry : array) {
            value.value().add(fromGson(entry));
        }
        return value;
    }
}
