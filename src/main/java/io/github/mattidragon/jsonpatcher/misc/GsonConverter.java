package io.github.mattidragon.jsonpatcher.misc;

import com.google.common.collect.Sets;
import com.google.gson.*;
import dev.mattidragon.jsonpatcher.lang.runtime.value.Value;

import java.util.Set;

public class GsonConverter {
    private static final ThreadLocal<Set<Value>> TO_GSON_RECURSION_TRACKER = ThreadLocal.withInitial(Sets::newIdentityHashSet);
    private static final ThreadLocal<Set<JsonElement>> FROM_GSON_RECURSION_TRACKER = ThreadLocal.withInitial(Sets::newIdentityHashSet);

    private GsonConverter() {
    }

    public static JsonElement toGson(Value value) {
        try {
            if (!TO_GSON_RECURSION_TRACKER.get().add(value)) {
                throw new IllegalStateException("recursive value tree");
            }
            return switch (value) {
                case Value.ObjectValue objectValue -> toGson(objectValue);
                case Value.ArrayValue arrayValue -> toGson(arrayValue);
                case Value.NumberValue(var num) -> new JsonPrimitive(num);
                case Value.StringValue(var s) -> new JsonPrimitive(s);
                case Value.BooleanValue booleanValue -> new JsonPrimitive(booleanValue.value());
                case Value.NullValue.NULL -> JsonNull.INSTANCE;
                case null, default -> throw new IllegalStateException("Can't convert %s to gson".formatted(value));
            };
        } finally {
            TO_GSON_RECURSION_TRACKER.get().remove(value);
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
            if (!FROM_GSON_RECURSION_TRACKER.get().add(json)) {
                throw new IllegalStateException("recursive gson json tree");
            }
            return switch (json) {
                case JsonObject jsonObject -> fromGson(jsonObject);
                case JsonArray jsonArray -> fromGson(jsonArray);
                case JsonPrimitive primitive when primitive.isBoolean() -> Value.BooleanValue.of(primitive.getAsBoolean());
                case JsonPrimitive primitive when primitive.isNumber() -> new Value.NumberValue(primitive.getAsNumber().doubleValue());
                case JsonPrimitive primitive when primitive.isString() -> new Value.StringValue(primitive.getAsString());
                case JsonNull jsonNull -> Value.NullValue.NULL;
                case null, default -> throw new IllegalStateException("Can't convert %s to value".formatted(json));
            };
        } finally {
            FROM_GSON_RECURSION_TRACKER.get().remove(json);
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
