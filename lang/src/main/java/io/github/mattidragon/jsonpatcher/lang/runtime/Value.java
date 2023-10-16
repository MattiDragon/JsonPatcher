package io.github.mattidragon.jsonpatcher.lang.runtime;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.function.PatchFunction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public sealed interface Value {

    boolean asBoolean();

    record ObjectValue(Map<String, Value> value) implements Value {
        public ObjectValue {
            value = new LinkedHashMap<>(value);
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
        public String toString() {
            return value.toString();
        }
    }

    record ArrayValue(List<Value> value) implements Value {
        public ArrayValue {
            value = new ArrayList<>(value);
        }

        public ArrayValue() {
            this(List.of());
        }

        @Override
        public boolean asBoolean() {
            return !value.isEmpty();
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

    }

    sealed interface Primitive extends Value {}

    record StringValue(String value) implements Primitive {
        @Override
        public boolean asBoolean() {
            return !value.isEmpty();
        }

        @Override
        public String toString() {
            return "\"" + value + "\"";
        }
    }

    record NumberValue(double value) implements Primitive {
        @Override
        public boolean asBoolean() {
            return value != 0;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
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
        public String toString() {
            return String.valueOf(value());
        }
    }

    enum NullValue implements Primitive {
        NULL;

        @Override
        public boolean asBoolean() {
            return false;
        }

        @Override
        public String toString() {
            return "null";
        }
    }
}
