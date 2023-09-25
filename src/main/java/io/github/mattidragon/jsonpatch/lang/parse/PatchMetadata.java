package io.github.mattidragon.jsonpatch.lang.parse;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import io.github.mattidragon.jsonpatch.patch.PatchTarget;

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

    public void put(String keyName, String data, SourcePos pos) {
        var key = lookup.findKey(keyName);
        if (key.isEmpty()) throw new Lexer.LexException("Unknown meta tag: '@%s'".formatted(keyName), pos);
        values.put(key.get(), lookup.parsers.get(key.get()).parse(data, pos));
    }

    public static class Key<T> {
        private final String name;

        public Key(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class ParserLookup {
        private final Map<Key<?>, Parser<?>> parsers = new HashMap<>();

        public <T> ParserLookup put(Key<T> key, Parser<T> value) {
            parsers.put(key, value);
            return this;
        }

        private Optional<Key<?>> findKey(String keyName) {
            for (var key : parsers.keySet()) {
                if (key.getName().equals(keyName)) {
                    return Optional.of(key);
                }
            }
            return Optional.empty();
        }
    }

    public interface Parser<T> {
        T parse(String data, SourcePos pos);
    }

    // In the future: make this actually store script version instead of just validating
    public record Version() {
        public static final Key<Version> KEY = new Key<>("version");
        public static final Parser<Version> PARSER = (data, pos) -> {
            if (!data.equals("1")) throw new IllegalStateException("Unsupported version: '%s'".formatted(data));
            return new Version();
        };
    }

    public record Target(PatchTarget target) {
        public static final Key<Target> KEY = new Key<>("target");
        public static final Parser<Target> PARSER = (data, pos) -> new Target(PatchTarget.parse(data, pos));
    }
}
