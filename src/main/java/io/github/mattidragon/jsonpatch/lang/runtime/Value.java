package io.github.mattidragon.jsonpatch.lang.runtime;

import com.google.gson.*;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import org.jetbrains.annotations.Nullable;

public sealed interface Value {
    static Value of(JsonElement element) {
        if (element instanceof JsonObject object) return new ObjectValue(object);
        if (element instanceof JsonArray array) return new ArrayValue(array);
        if (element instanceof JsonNull) return NullValue.NULL;
        if (element instanceof JsonPrimitive primitive) {
            if (primitive.isBoolean()) return BooleanValue.of(primitive.getAsBoolean());
            if (primitive.isNumber()) return new NumberValue(primitive.getAsNumber().doubleValue());
            if (primitive.isString()) return new StringValue(primitive.getAsString());
        }
        throw new IllegalStateException("Unsupported json element: " + element);
    }

    boolean asBoolean();

    JsonElement toGson();

    record ObjectValue(JsonObject value) implements Value {
        public Value get(String key, @Nullable SourceSpan pos) {
            if (!value.has(key)) throw new EvaluationException("Object %s has no key %s".formatted(this, key), pos);
            return Value.of(value.get(key));
        }

        public void set(String key, Value value, @Nullable SourceSpan pos) {
            this.value.add(key, value.toGson());
        }

        @Override
        public boolean asBoolean() {
            return value.size() > 0;
        }

        @Override
        public JsonElement toGson() {
            return value;
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    record ArrayValue(JsonArray value) implements Value {
        @Override
        public boolean asBoolean() {
            return !value.isEmpty();
        }

        @Override
        public JsonElement toGson() {
            return value;
        }

        @Override
        public String toString() {
            return value.toString();
        }

        public Value get(int index, @Nullable SourceSpan pos) {
            return Value.of(this.value.get(fixIndex(index, pos)));
        }

        public void set(int index, Value value, @Nullable SourceSpan pos) {
            this.value.set(fixIndex(index, pos), value.toGson());
        }

        private int fixIndex(int index, @Nullable SourceSpan pos) {
            if (index >= value.size() || index < -value.size())
                throw new EvaluationException("Array index out of bounds (index: %s, size: %s)".formatted(index, value.size()), pos);
            if (index < 0) return value.size() + index;
            return index;
        }
    }

    sealed interface Primitive extends Value {}

    record StringValue(String value) implements Primitive {
        @Override
        public boolean asBoolean() {
            return !value.isEmpty();
        }

        @Override
        public JsonElement toGson() {
            return new JsonPrimitive(value());
        }

        @Override
        public String toString() {
            return toGson().toString();
        }
    }

    record NumberValue(double value) implements Primitive {
        @Override
        public boolean asBoolean() {
            return value != 0;
        }

        @Override
        public JsonElement toGson() {
            return new JsonPrimitive(value());
        }

        @Override
        public String toString() {
            return toGson().toString();
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
        public JsonElement toGson() {
            return new JsonPrimitive(value());
        }

        @Override
        public String toString() {
            return toGson().toString();
        }
    }

    enum NullValue implements Primitive {
        NULL;

        @Override
        public boolean asBoolean() {
            return false;
        }

        @Override
        public JsonElement toGson() {
            return JsonNull.INSTANCE;
        }

        @Override
        public String toString() {
            return toGson().toString();
        }
    }
}
