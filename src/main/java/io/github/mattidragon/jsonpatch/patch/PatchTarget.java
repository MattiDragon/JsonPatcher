package io.github.mattidragon.jsonpatch.patch;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public record PatchTarget(List<Pattern> positive, List<Pattern> negative) implements Predicate<Identifier> {
    public PatchTarget {
        positive = List.copyOf(positive);
        negative = List.copyOf(negative);
    }

    public static PatchTarget parse(String code) {
        var patterns = code.trim().split(" ");
        var positivePatterns = new ArrayList<Pattern>();
        var negativePatterns = new ArrayList<Pattern>();

        for (var pattern : patterns) {
            if (pattern.isBlank()) continue;

            var original = pattern;

            var negative = pattern.startsWith("!");
            if (negative) pattern = pattern.substring(1);

            var namespaceSeparatorIndex = pattern.indexOf(':');
            var namespace = parseNamespace(pattern, namespaceSeparatorIndex, original);
            var path = parsePath(pattern, namespaceSeparatorIndex, original);

            if (negative) {
                negativePatterns.add(new Pattern(namespace, path));
            } else {
                positivePatterns.add(new Pattern(namespace, path));
            }
        }

        return new PatchTarget(positivePatterns, negativePatterns);
    }

    private static Optional<String> parseNamespace(String pattern, int namespaceSeparatorIndex, String original) {
        if (namespaceSeparatorIndex == -1 || pattern.indexOf(':', namespaceSeparatorIndex + 1) != -1)
            throw new IllegalStateException("Invalid target pattern: %s. Found zero or multiple colons.".formatted(original));

        var namespaceString = pattern.substring(0, namespaceSeparatorIndex);

        Optional<String> namespace;
        if (namespaceString.equals("*")) {
            namespace = Optional.empty();
        } else {
            if (namespaceString.indexOf('*') != -1)
                throw new IllegalStateException("Invalid target pattern: %s. Wildcard namespaces don't support prefixes or suffixes.".formatted(original));
            namespace = Optional.of(namespaceString);
        }
        return namespace;
    }

    private static ArrayList<Segment> parsePath(String pattern, int namespaceSeparatorIndex, String original) {
        var path = namespaceSeparatorIndex == -1 ? pattern : pattern.substring(namespaceSeparatorIndex + 1);
        var pathSegments = path.split("/");
        var finalPath = new ArrayList<Segment>();

        for (var segment : pathSegments) {
            var wildcardIndex = segment.indexOf('*');
            if (wildcardIndex == -1) {
                finalPath.add(new Segment.Named(segment));
            } else {
                if (segment.indexOf('*', wildcardIndex + 1) != -1)
                    throw new IllegalStateException("Invalid target pattern: %s. Found multiple wildcards in one segment.".formatted(original));
                finalPath.add(new Segment.Wildcard(segment.substring(0, wildcardIndex), segment.substring(wildcardIndex + 1)));
            }
        }
        return finalPath;
    }

    @Override
    public boolean test(Identifier identifier) {
        return positive.stream().anyMatch(pattern -> pattern.test(identifier)) && negative.stream().noneMatch(positive -> positive.test(identifier));
    }

    public record Pattern(Optional<String> namespace, List<Segment> path) implements Predicate<Identifier> {
        public Pattern {
            path = List.copyOf(path);
        }

        @Override
        public boolean test(Identifier identifier) {
            if (!namespace.map(identifier.getNamespace()::matches).orElse(true)) return false;
            var segments = identifier.getPath().split("/");
            if (segments.length != path.size()) return false;
            for (int i = 0; i < segments.length; i++) {
                if (!path.get(i).test(segments[i])) return false;
            }
            return true;
        }
    }

    public sealed interface Segment extends Predicate<String> {
        record Wildcard(String prefix, String suffix) implements Segment {
            @Override
            public boolean test(String s) {
                return s.startsWith(prefix) && s.endsWith(suffix) && s.length() >= prefix.length() + suffix.length();
            }
        }
        record Named(String value) implements Segment {
            @Override
            public boolean test(String s) {
                return value.equals(s);
            }
        }
    }
}
