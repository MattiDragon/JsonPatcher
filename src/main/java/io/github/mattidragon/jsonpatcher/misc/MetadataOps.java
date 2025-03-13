package io.github.mattidragon.jsonpatcher.misc;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.mattidragon.jsonpatcher.lang.parse.metadata.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class MetadataOps implements DynamicOps<MetadataElement> {
    public static final MetadataOps INSTANCE = new MetadataOps();

    private MetadataOps() {}

    @Override
    public MetadataElement empty() {
        return new MetadataNull();
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, MetadataElement input) {
        return switch (input) {
            case MetadataNull() -> outOps.empty();
            case MetadataBoolean(boolean value) -> outOps.createBoolean(value);
            case MetadataNumber(double value) -> outOps.createNumeric(value);
            case MetadataString(String value) -> outOps.createString(value);
            case MetadataArray(List<MetadataElement> values) ->
                    outOps.createList(values.stream().map(value -> convertTo(outOps, value)));
            case MetadataObject(Map<String, MetadataElement> values) ->
                    outOps.createMap(values.entrySet().stream().map(entry -> Pair.of(outOps.createString(entry.getKey()), convertTo(outOps, entry.getValue()))));
        };
    }

    @Override
    public DataResult<Number> getNumberValue(MetadataElement input) {
        if (input instanceof MetadataNumber(double value)) {
            return DataResult.success(value);
        }
        return DataResult.error(() -> "Not a number: " + input);
    }

    @Override
    public MetadataElement createNumeric(Number i) {
        return new MetadataNumber(i.doubleValue());
    }

    @Override
    public DataResult<String> getStringValue(MetadataElement input) {
        if (input instanceof MetadataString(String value)) {
            return DataResult.success(value);
        }
        return DataResult.error(() -> "Not a string: " + input);
    }

    @Override
    public MetadataElement createString(String value) {
        return new MetadataString(value);
    }

    @Override
    public DataResult<Boolean> getBooleanValue(MetadataElement input) {
        if (input instanceof MetadataBoolean(boolean value)) {
            return DataResult.success(value);
        }
        return DataResult.error(() -> "Not a boolean: " + input);
    }

    @Override
    public MetadataElement createBoolean(boolean value) {
        return new MetadataBoolean(value);
    }

    @Override
    public DataResult<MetadataElement> mergeToList(MetadataElement list, MetadataElement value) {
        if (!(list instanceof MetadataArray(List<MetadataElement> values))) {
            return DataResult.error(() -> "Not a list: " + list);
        }
        var newValues = new ArrayList<>(values);
        newValues.add(value);
        return DataResult.success(new MetadataArray(newValues));
    }

    @Override
    public DataResult<MetadataElement> mergeToMap(MetadataElement map, MetadataElement key, MetadataElement value) {
        if (!(map instanceof MetadataObject(Map<String, MetadataElement> values))) {
            return DataResult.error(() -> "Not a map: " + map);
        }
        if (!(key instanceof MetadataString(String keyString))) {
            return DataResult.error(() -> "Not a string: " + key);
        }

        var newValues = new HashMap<>(values);
        newValues.put(keyString, value);
        return DataResult.success(new MetadataObject(newValues));
    }

    @Override
    public DataResult<Stream<Pair<MetadataElement, MetadataElement>>> getMapValues(MetadataElement input) {
        if (!(input instanceof MetadataObject(Map<String, MetadataElement> values))) {
            return DataResult.error(() -> "Not a map: " + input);
        }
        return DataResult.success(values.entrySet()
                .stream()
                .map(entry -> Pair.of(new MetadataString(entry.getKey()), entry.getValue())));
    }

    @Override
    public MetadataElement createMap(Stream<Pair<MetadataElement, MetadataElement>> map) {
        var values = new HashMap<String, MetadataElement>();
        map.forEach(pair -> {
            if (!(pair.getFirst() instanceof MetadataString(String key))) {
                throw new IllegalArgumentException("Key is not a string: " + pair.getFirst());
            }
            values.put(key, pair.getSecond());
        });
        return new MetadataObject(values);
    }

    @Override
    public DataResult<Stream<MetadataElement>> getStream(MetadataElement input) {
        if (!(input instanceof MetadataArray(List<MetadataElement> values))) {
            return DataResult.error(() -> "Not an array: " + input);
        }
        return DataResult.success(values.stream());
    }

    @Override
    public MetadataElement createList(Stream<MetadataElement> input) {
        return new MetadataArray(input.toList());
    }

    @Override
    public MetadataElement remove(MetadataElement input, String key) {
        if (!(input instanceof MetadataObject(Map<String, MetadataElement> values))) {
            return input;
        }
        var newValues = new HashMap<>(values);
        newValues.remove(key);
        return new MetadataObject(newValues);
    }
}
