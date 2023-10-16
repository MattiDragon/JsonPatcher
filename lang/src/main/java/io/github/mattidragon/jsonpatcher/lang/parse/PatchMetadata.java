package io.github.mattidragon.jsonpatcher.lang.parse;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

// Horrible generics crime in order to have a clean interface on the outside
public class PatchMetadata {
    private final Map<Key<?>, Object> values = new LinkedHashMap<>();
    private final ParserLookup lookup;

    public PatchMetadata(ParserLookup lookup) {
        this.lookup = lookup;
    }

    public <T> T get(Key<T> key) {
        //noinspection unchecked
        return (T) values.get(key);
    }

    public <T> T expect(Key<T> key) {
        var value = get(key);
        if (value == null) throw new IllegalStateException("Missing metadata: @%s".formatted(key.name));
        return value;
    }

    public <T> Optional<T> getOrEmpty(Key<T> key) {
        var value = get(key);
        if (value == null) return Optional.empty();
        return Optional.of(value);
    }

    public void add(String keyName, Parser parser) {
        var key = lookup.findKey(keyName);
        if (key.isEmpty()) throw new Parser.ParseException("Unknown meta tag: '@%s'".formatted(keyName), parser.previous().getPos());
        values.put(key.get(), getAndParse(parser, key.get()));
    }

    private <T> T getAndParse(Parser parser, Key<T> key) {
        //noinspection unchecked
        var metaParser = (MetaParser<T>) lookup.parsers.get(key);
        return metaParser.parse(parser, get(key));
    }

    public record Key<T>(String name) {
    }

    public static class ParserLookup {
        private final Map<Key<?>, MetaParser<?>> parsers = new HashMap<>();

        public <T> ParserLookup put(Key<T> key, MetaParser<T> value) {
            parsers.put(key, value);
            return this;
        }

        private Optional<Key<?>> findKey(String keyName) {
            for (var key : parsers.keySet()) {
                if (key.name().equals(keyName)) {
                    return Optional.of(key);
                }
            }
            return Optional.empty();
        }
    }

    public interface MetaParser<T> {
        T parse(Parser parser, @Nullable T previous);

        interface SingleValueMetaParser<T> extends MetaParser<T> {
            @Override
            default T parse(Parser parser, @Nullable T previous) {
                if (previous != null) {
                    var name = parser.previous().getToken() instanceof Token.WordToken word ? word.value() : "(unknown)";
                    throw new Parser.ParseException("Duplicate meta tag: @%s".formatted(name), parser.previous().getPos());
                }
                return parse(parser);
            }

            T parse(Parser parser);
        }
    }

    // In the future: make this actually store script version instead of just validating
    public record Version() {
        public static final Key<Version> KEY = new Key<>("version");
        public static final MetaParser.SingleValueMetaParser<Version> PARSER = parser -> {
            var version = parser.next();
            if (!(version.getToken() instanceof Token.NumberToken number))
                return parser.expectFail("number");
            if (number.value() != 1) throw new IllegalStateException("Unsupported version: '%s'".formatted(number));
            return new Version();
        };
    }
}
