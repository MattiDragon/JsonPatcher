package io.github.mattidragon.jsonpatcher.lang.parse;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import io.github.mattidragon.jsonpatcher.patch.PatchTarget;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// Horrible generics crime in order to have a clean interface on the outside
public class PatchMetadata {
    private final Multimap<Key<?>, Object> values = LinkedHashMultimap.create();
    private final ParserLookup lookup;

    public PatchMetadata(ParserLookup lookup) {
        this.lookup = lookup;
    }

    public <T> Collection<T> get(Key<T> key) {
        //noinspection unchecked
        return (Collection<T>) values.get(key);
    }

    public <T> T expectSingle(Key<T> key) {
        var all = get(key);
        if (all.isEmpty()) throw new IllegalStateException("Missing metadata: @%s".formatted(key.name));
        if (all.size() > 1) throw new IllegalStateException("Duplicate metadata: @%s".formatted(key.name));
        return all.iterator().next();
    }

    public <T> Optional<T> expectSingleOrNone(Key<T> key) {
        var all = get(key);
        if (all.isEmpty()) return Optional.empty();
        if (all.size() > 1) throw new IllegalStateException("Duplicate metadata: @%s".formatted(key.name));
        return Optional.of(all.iterator().next());
    }

    public void add(String keyName, Parser parser) {
        var key = lookup.findKey(keyName);
        if (key.isEmpty()) throw new Parser.ParseException("Unknown meta tag: '@%s'".formatted(keyName), parser.previous().getPos());
        values.put(key.get(), lookup.parsers.get(key.get()).parse(parser));
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
        T parse(Parser parser);
    }

    // In the future: make this actually store script version instead of just validating
    public record Version() {
        public static final Key<Version> KEY = new Key<>("version");
        public static final MetaParser<Version> PARSER = parser -> {
            var version = parser.next();
            if (!(version.getToken() instanceof Token.NumberToken number))
                return parser.expectFail("number");
            if (number.value() != 1) throw new IllegalStateException("Unsupported version: '%s'".formatted(number));
            return new Version();
        };
    }

    public record Target(PatchTarget target) {
        public static final Key<Target> KEY = new Key<>("target");
        public static final MetaParser<Target> PARSER = parser -> new Target(PatchTarget.parse(parser));
    }
}
