package io.github.mattidragon.jsonpatcher.lang.parse;

import io.github.mattidragon.jsonpatcher.lang.PositionedException;
import io.github.mattidragon.jsonpatcher.lang.parse.parselet.PostfixParselet;
import io.github.mattidragon.jsonpatcher.lang.parse.parselet.Precedence;
import io.github.mattidragon.jsonpatcher.lang.parse.parselet.PrefixParser;
import io.github.mattidragon.jsonpatcher.lang.parse.parselet.StatementParser;
import io.github.mattidragon.jsonpatcher.lang.runtime.Program;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.ErrorExpression;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Expression;
import io.github.mattidragon.jsonpatcher.lang.runtime.statement.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<PositionedToken<?>> tokens;
    private final List<ParseException> errors = new ArrayList<>();
    private final PatchMetadata metadata;
    private int current = 0;

    public Parser(List<PositionedToken<?>> tokens, PatchMetadata.ParserLookup lookup) {
        this.tokens = tokens;
        this.metadata = new PatchMetadata(lookup);
    }

    public ParseResult program() {
        while (hasNext(Token.SimpleToken.AT_SIGN)) {
            next();
            var id = expectWord().value();
            metadata.add(id, this);
            expect(Token.SimpleToken.SEMICOLON);
        }

        var statements = new ArrayList<Statement>();
        try {
            while (hasNext())
                statements.add(statement());
        } catch (ParseException e) {
            errors.add(e);
        } catch (EndParsingException ignored) {}

        if (!errors.isEmpty()) {
            return new ParseResult.Fail(errors);
        }
        return new ParseResult.Success(new Program(statements), metadata);
    }

    private Statement statement() {
        return StatementParser.parse(this);
    }

    public Expression expression() {
        return expression(Precedence.ROOT);
    }

    public Expression expression(Precedence precedence) {
        Expression left;
        try {
            left = PrefixParser.parse(this, next());
        } catch (ParseException e) {
            errors.add(e);
            left = new ErrorExpression(e);
        }

        while (true) {
            var postfix = PostfixParselet.get(peek());
            if (postfix == null) break;
            if (precedence.ordinal() >= postfix.precedence().ordinal()) break;
            var postfixToken = next();
            try {
                left = postfix.parse(this, left, postfixToken);
            } catch (ParseException e) {
                errors.add(e);
                left = new ErrorExpression(e);
            }
        }

        return left;
    }

    public void seek(Token token) {
        while (hasNext() && peek().getToken() != token) next();
        expect(token);
    }

    public Token.WordToken expectWord() {
        var token = next().getToken();
        if (token instanceof Token.WordToken wordToken) return wordToken;
        return expectFail("word");
    }

    public Token.StringToken expectString() {
        var token = next().getToken();
        if (token instanceof Token.StringToken stringToken) return stringToken;
        return expectFail("string");
    }

    public Token.NumberToken expectNumber() {
        var token = next().getToken();
        if (token instanceof Token.NumberToken numberToken) return numberToken;
        return expectFail("number");
    }

    public void expect(Token token) {
        var found = next().getToken();
        if (found != token) expectFail(token.toString());
    }

    @Contract("_ -> fail")
    public <T> T expectFail(String expected) {
        throw new ParseException("Expected %s, but found %s".formatted(expected, previous().getToken()), previous().getPos());
    }

    public void addError(ParseException error) {
        errors.add(error);
    }

    public PositionedToken<?> next() {
        if (!hasNext()) {
            errors.add(new ParseException("Unexpected end of file", new SourceSpan(previous().getTo(), previous().getTo())));
            throw new EndParsingException();
        }
        return tokens.get(current++);
    }

    public PositionedToken<?> previous() {
        if (current == 0) throw new IllegalStateException("No previous token (the parser is broken)");
        return tokens.get(current - 1);
    }

    public PositionedToken<?> peek() {
        if (!hasNext()) {
            errors.add(new ParseException("Unexpected end of file", new SourceSpan(previous().getTo(), previous().getTo())));
            throw new EndParsingException();
        }
        return tokens.get(current);
    }

    public boolean hasNext() {
        return current < tokens.size();
    }

    public boolean hasNext(Token token) {
        return hasNext() && peek().getToken() == token;
    }

    /**
     * Special error to throw when we reach an error condition from which recovery doesn't make sense (end of file)
     */
    private static class EndParsingException extends RuntimeException {
    }

    public static class ParseException extends PositionedException {
        public final SourceSpan pos;

        public ParseException(String message, SourceSpan pos) {
            super(message);
            this.pos = pos;
        }

        @Override
        protected String getBaseMessage() {
            return "Error while parsing patch";
        }

        @Override
        protected @Nullable SourceSpan getPos() {
            return pos;
        }
    }
}
