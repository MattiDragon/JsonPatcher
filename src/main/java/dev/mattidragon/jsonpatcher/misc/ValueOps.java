package dev.mattidragon.jsonpatcher.misc;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.mattidragon.jsonpatcher.lang.runtime.value.Value;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValueOps implements DynamicOps<Value> {
    public static final ValueOps INSTANCE = new ValueOps();

    private ValueOps() {}

    @Override
    public Value empty() {
        return Value.NullValue.NULL;
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, Value input) {
        return switch (input) {
            case Value.NullValue nullValue -> outOps.empty();
            case Value.BooleanValue booleanValue -> outOps.createBoolean(booleanValue.value());
            case Value.NumberValue(var value) -> outOps.createNumeric(value);
            case Value.StringValue(var value) -> outOps.createString(value);
            case Value.ArrayValue arrayValue ->
                    outOps.createList(arrayValue.value().stream().map(value -> convertTo(outOps, value)));
            case Value.ObjectValue objectValue ->
                    outOps.createMap(objectValue.value().entrySet().stream().map(entry -> Pair.of(outOps.createString(entry.getKey()), convertTo(outOps, entry.getValue()))));
            case Value.FunctionValue functionValue -> throw new IllegalStateException("Cannot convert function value");
            case null, default -> throw new IllegalStateException("Unknown value type: " + input);
        };
    }

    @Override
    public DataResult<Number> getNumberValue(Value input) {
        if (input instanceof Value.NumberValue(var value)) {
            return DataResult.success(value);
        }
        return DataResult.error(() -> "Not a number: " + input);
    }

    @Override
    public Value createNumeric(Number i) {
        return new Value.NumberValue(i.doubleValue());
    }

    @Override
    public DataResult<String> getStringValue(Value input) {
        if (input instanceof Value.StringValue(var value)) {
            return DataResult.success(value);
        }
        return DataResult.error(() -> "Not a string: " + input);
    }

    @Override
    public Value createString(String value) {
        return new Value.StringValue(value);
    }

    @Override
    public DataResult<Value> mergeToList(Value list, Value value) {
        if (list instanceof Value.ArrayValue array) {
            var result = new Value.ArrayValue(array.value());
            result.value().add(value);
            return DataResult.success(result);
        }
        return DataResult.error(() -> "Not an array: " + list);
    }

    @Override
    public DataResult<Value> mergeToMap(Value map, Value key, Value value) {
        if (map instanceof Value.ObjectValue object) {
            var result = new Value.ObjectValue(object.value());
            result.value().put(key.toString(), value);
            return DataResult.success(result);
        }
        return DataResult.error(() -> "Not an object: " + map);
    }

    @Override
    public DataResult<Stream<Pair<Value, Value>>> getMapValues(Value input) {
        if (input instanceof Value.ObjectValue objectValue) {
            return DataResult.success(objectValue.value().entrySet().stream().map(entry -> Pair.of(new Value.StringValue(entry.getKey()), entry.getValue())));
        }
        return DataResult.error(() -> "Not an object: " + input);
    }

    @Override
    public Value createMap(Stream<Pair<Value, Value>> map) {
        return new Value.ObjectValue(map.collect(Collectors.<Pair<Value, Value>, String, Value>toMap(pair -> pair.getFirst().toString(), Pair::getSecond)));
    }

    @Override
    public DataResult<Stream<Value>> getStream(Value input) {
        if (input instanceof Value.ArrayValue arrayValue) {
            return DataResult.success(arrayValue.value().stream());
        }
        return DataResult.error(() -> "Not an array: " + input);
    }

    @Override
    public Value createList(Stream<Value> input) {
        return new Value.ArrayValue(input.toList());
    }

    @Override
    public Value remove(Value input, String key) {
        if (input instanceof Value.ObjectValue objectValue) {
            var newValue = new Value.ObjectValue();
            objectValue.value().forEach((k, v) -> {
                if (!k.equals(key)) newValue.value().put(k, v);
            });
            return newValue;
        }
        return input;
    }
}
