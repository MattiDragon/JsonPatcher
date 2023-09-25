package io.github.mattidragon.jsonpatch.lang.runtime;

import com.google.gson.*;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatch.lang.runtime.function.PatchFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public sealed interface Value {
    Set<Value> TO_GSON_RECURSION_TRACKER = Collections.newSetFromMap(new WeakHashMap<>());
    Set<JsonElement> FROM_GSON_RECURSION_TRACKER = Collections.newSetFromMap(new WeakHashMap<>());

    static Value of(JsonElement element) {
        if (!FROM_GSON_RECURSION_TRACKER.add(element)) {
            throw new IllegalStateException("recursive gson json tree");
        }

        try {
            if (element instanceof JsonObject object) return new ObjectValue(object);
            if (element instanceof JsonArray array) return new ArrayValue(array);
            if (element instanceof JsonNull) return NullValue.NULL;
            if (element instanceof JsonPrimitive primitive) {
                if (primitive.isBoolean()) return BooleanValue.of(primitive.getAsBoolean());
                if (primitive.isNumber()) return new NumberValue(primitive.getAsNumber().doubleValue());
                if (primitive.isString()) return new StringValue(primitive.getAsString());
            }
            throw new IllegalStateException("Unsupported json element: " + element);
        } finally {
            FROM_GSON_RECURSION_TRACKER.remove(element);
        }
    }

    boolean asBoolean();

    JsonElement toGson(SourceSpan pos);

    record ObjectValue(Map<String, Value> value) implements Value {
        public ObjectValue {
            value = new LinkedHashMap<>(value);
        }

        public ObjectValue(JsonObject object) {
            this();
            object.asMap().forEach((key, value) -> this.value.put(key, Value.of(value)));
        }

        public ObjectValue() {
            this(Map.of());
        }

        public Value get(String key, @Nullable SourceSpan pos) {
            if (!value.containsKey(key)) throw new EvaluationException("Object %s has no key %s".formatted(this, key), pos);
            return value.get(key);
        }

        public void set(String key, Value value, @Nullable SourceSpan pos) {
            this.value.put(key, value);
        }

        public void remove(String key, SourceSpan pos) {
            if (!value.containsKey(key)) throw new EvaluationException("Object %s has no key %s".formatted(this, key), pos);
            value.remove(key);
        }

        @Override
        public boolean asBoolean() {
            return !value.isEmpty();
        }

        @Override
        public JsonObject toGson(SourceSpan pos) {
            if (!TO_GSON_RECURSION_TRACKER.add(this)) {
                throw new EvaluationException("Recursive json tree", pos);
            }
            try {
                var json = new JsonObject();
                value.forEach((key, value) -> json.add(key, value.toGson(pos)));
                return json;
            } finally {
                TO_GSON_RECURSION_TRACKER.remove(this);
            }
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    record ArrayValue(List<Value> value) implements Value {
        public ArrayValue {
            value = new ArrayList<>(value);
        }

        public ArrayValue(JsonArray array) {
            this();
            array.asList()
                    .stream()
                    .map(Value::of)
                    .forEach(value::add);
        }

        public ArrayValue() {
            this(List.of());
        }

        @Override
        public boolean asBoolean() {
            return !value.isEmpty();
        }

        @Override
        public JsonArray toGson(SourceSpan pos) {
            if (!TO_GSON_RECURSION_TRACKER.add(this)) {
                throw new EvaluationException("Recursive json tree", pos);
            }
            try {
                var json = new JsonArray();
                value.forEach((value) -> json.add(value.toGson(pos)));
                return json;
            } finally {
                TO_GSON_RECURSION_TRACKER.remove(this);
            }
        }

        @Override
        public String toString() {
            return value.toString();
        }

        public Value get(int index, @Nullable SourceSpan pos) {
            return this.value.get(fixIndex(index, pos));
        }

        public void set(int index, Value value, @Nullable SourceSpan pos) {
            this.value.set(fixIndex(index, pos), value);
        }

        public void remove(int index, SourceSpan pos) {
            value.remove(fixIndex(index, pos));
        }

        private int fixIndex(int index, @Nullable SourceSpan pos) {
            if (index >= value.size() || index < -value.size())
                throw new EvaluationException("Array index out of bounds (index: %s, size: %s)".formatted(index, value.size()), pos);
            if (index < 0) return value.size() + index;
            return index;
        }
    }

    record FunctionValue(PatchFunction function) implements Value {
        @Override
        public boolean asBoolean() {
            return true;
        }

        @Override
        public JsonElement toGson(SourceSpan pos) {
            throw new EvaluationException("Tried to convert function to json. Did you place a function in the main tree?", pos);
        }
    }

    sealed interface Primitive extends Value {}

    record StringValue(String value) implements Primitive {
        @Override
        public boolean asBoolean() {
            return !value.isEmpty();
        }

        @Override
        public JsonElement toGson(SourceSpan pos) {
            return new JsonPrimitive(value());
        }

        @Override
        public String toString() {
            return toGson(null).toString();
        }
    }

    record NumberValue(double value) implements Primitive {
        @Override
        public boolean asBoolean() {
            return value != 0;
        }

        @Override
        public JsonElement toGson(SourceSpan pos) {
            return new JsonPrimitive(value());
        }

        @Override
        public String toString() {
            return toGson(null).toString();
        }
    }

    enum BooleanValue implements Primitive {
        TRUE, FALSE;

        public static BooleanValue of(boolean value) {
            return value ? TRUE : FALSE;
        }

        public boolean value() {
            return this == TRUE;
        }

        @Override
        public boolean asBoolean() {
            return value();
        }

        @Override
        public JsonElement toGson(SourceSpan pos) {
            return new JsonPrimitive(value());
        }

        @Override
        public String toString() {
            return toGson(null).toString();
        }
    }

    enum NullValue implements Primitive {
        NULL;

        @Override
        public boolean asBoolean() {
            return false;
        }

        @Override
        public JsonElement toGson(SourceSpan pos) {
            return JsonNull.INSTANCE;
        }

        @Override
        public String toString() {
            return toGson(null).toString();
        }
    }
}
