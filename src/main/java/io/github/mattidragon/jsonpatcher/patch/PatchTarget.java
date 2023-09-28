package io.github.mattidragon.jsonpatcher.patch;

import io.github.mattidragon.jsonpatcher.lang.parse.Parser;
import io.github.mattidragon.jsonpatcher.lang.parse.PositionedToken;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.parse.Token;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public record PatchTarget(
        Optional<String> namespace,
        Optional<String> pathStart,
        Optional<String> pathEnd,
        Optional<String> path,
        Optional<Pattern> regex) implements Predicate<Identifier> {
    public static PatchTarget parse(Parser parser) {
        MutableObject<String> namespace = new MutableObject<>();
        MutableObject<String> pathStart = new MutableObject<>();
        MutableObject<String> pathEnd = new MutableObject<>();
        MutableObject<String> path = new MutableObject<>();
        MutableObject<String> regex = new MutableObject<>();
        boolean hasNormal = false;
        boolean all = false;

        var fromPos = parser.peek().getFrom();
        while (parser.peek() instanceof PositionedToken.WordToken token) {
            if (token.getToken().value().equals("all")) {
                all = true;
                continue;
            }

            var location = switch (token.getToken().value()) {
                case "namespace" -> namespace;
                case "path_start" -> pathStart;
                case "path_end" -> pathEnd;
                case "path" -> path;
                case "regex" -> regex;
                default -> throw new Parser.ParseException("Unexpected selector in target: '%s'".formatted(token.getToken().value()), token.getPos());
            };
            if (location.getValue() != null) {
                throw new Parser.ParseException("Duplicate entry in target for: %s".formatted(token.getToken().value()), parser.previous().getPos());
            }
            parser.expect(Token.SimpleToken.COLON);
            location.setValue(parser.expectString().value());
            hasNormal = true;
        }

        if (!all && !hasNormal) parser.addError(new Parser.ParseException("No selectors specified. Use 'all' for selecting every file", parser.previous().getPos()));
        if (all && hasNormal) parser.addError(new Parser.ParseException("Can't combine normal selectors with 'all'", new SourceSpan(fromPos, parser.previous().getTo())));

        PatchTarget target;
        try {
            target = new PatchTarget(
                    Optional.of(namespace.getValue()),
                    Optional.of(pathStart.getValue()),
                    Optional.of(pathEnd.getValue()),
                    Optional.of(path.getValue()),
                    Optional.of(regex.getValue()).map(Pattern::compile)
            );
        } catch (PatternSyntaxException e) {
            // add error to parser, but keep going in case of other errors
            parser.addError(new Parser.ParseException("Invalid regex in target: %s".formatted(regex.getValue()), new SourceSpan(fromPos, parser.previous().getTo())));
            target = new PatchTarget(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }
        return target;
    }

    @Override
    public boolean test(Identifier identifier) {
        return namespace.map(identifier.getNamespace()::equals).orElse(true)
                && pathStart.map(identifier.getPath()::startsWith).orElse(true)
                && pathEnd.map(identifier.getPath()::endsWith).orElse(true)
                && path.map(identifier.getPath()::equals).orElse(true)
                && regex.map(pattern -> pattern.matcher(identifier.toString())).map(Matcher::matches).orElse(true);
    }
}
