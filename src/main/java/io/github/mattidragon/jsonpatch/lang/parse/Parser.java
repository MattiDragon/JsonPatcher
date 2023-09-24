package io.github.mattidragon.jsonpatch.lang.parse;

import io.github.mattidragon.jsonpatch.lang.PositionedException;
import io.github.mattidragon.jsonpatch.lang.parse.pratt.PostfixParselet;
import io.github.mattidragon.jsonpatch.lang.parse.pratt.Precedence;
import io.github.mattidragon.jsonpatch.lang.parse.pratt.PrefixParselet;
import io.github.mattidragon.jsonpatch.lang.runtime.Program;
import io.github.mattidragon.jsonpatch.lang.runtime.expression.ErrorExpression;
import io.github.mattidragon.jsonpatch.lang.runtime.expression.Expression;
import io.github.mattidragon.jsonpatch.lang.runtime.expression.Reference;
import io.github.mattidragon.jsonpatch.lang.runtime.statement.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<PositionedToken<?>> tokens;
    private final List<ParseException> errors = new ArrayList<>();
    private int current = 0;

    public Parser(List<PositionedToken<?>> tokens) {
        this.tokens = tokens;
    }

    public ParseResult program() {
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
        return new ParseResult.Success(new Program(statements));
    }

    private Statement statement() {
        var token = peek();
        if (token.getToken() == Token.SimpleToken.BEGIN_CURLY) return blockStatement();
        if (token.getToken() == Token.KeywordToken.APPLY) return applyStatement();
        if (token.getToken() == Token.KeywordToken.IF) return ifStatement();
        if (token.getToken() == Token.KeywordToken.VAR) return variableStatement(true);
        if (token.getToken() == Token.KeywordToken.VAL) return variableStatement(false);
        if (token.getToken() == Token.KeywordToken.DELETE) return deleteStatement();
        if (token.getToken() == Token.KeywordToken.FUNCTION) return functionDeclaration();
        return expressionStatement();
    }

    private Statement blockStatement() {
        expect(Token.SimpleToken.BEGIN_CURLY);
        var beginPos = previous().getFrom();
        var statements = new ArrayList<Statement>();
        try {
            while (peek().getToken() != Token.SimpleToken.END_CURLY)
                statements.add(statement());
        } catch (ParseException e) {
            errors.add(e);
            seek(Token.SimpleToken.END_CURLY);
            return new ErrorStatement(e);
        }
        expect(Token.SimpleToken.END_CURLY);
        var endPos = previous().getTo();
        return new BlockStatement(statements, new SourceSpan(beginPos, endPos));
    }

    private Statement applyStatement() {
        expect(Token.KeywordToken.APPLY);
        var beginPos = previous().getFrom();
        expect(Token.SimpleToken.BEGIN_PAREN);
        var root = expression();
        expect(Token.SimpleToken.END_PAREN);
        var action = statement();
        var endPos = previous().getTo();
        return new ApplyStatement(root, action, new SourceSpan(beginPos, endPos));
    }

    private Statement ifStatement() {
        expect(Token.KeywordToken.IF);
        var beginPos = previous().getFrom();
        expect(Token.SimpleToken.BEGIN_PAREN);
        var condition = expression();
        expect(Token.SimpleToken.END_PAREN);
        var action = statement();
        Statement elseAction = null;
        if (hasNext() && peek().getToken() == Token.KeywordToken.ELSE) {
            next();
            elseAction = statement();
        }
        var endPos = previous().getTo();
        return new IfStatement(condition, action, elseAction, new SourceSpan(beginPos, endPos));
    }

    private Statement variableStatement(boolean mutable) {
        var begin = next().getFrom();
        expect(Token.SimpleToken.DOLLAR);
        var name = expectWord();
        expect(Token.SimpleToken.ASSIGN);
        var initializer = expression();
        expect(Token.SimpleToken.SEMICOLON);
        return new VariableCreationStatement(name.value(), initializer, mutable, new SourceSpan(begin, previous().getTo()));
    }

    private Statement deleteStatement() {
        var begin = next().getFrom();
        var expression = expression();
        if (!(expression instanceof Reference ref)) throw new Parser.ParseException("Can't delete to %s".formatted(expression), expression.getPos());
        expect(Token.SimpleToken.SEMICOLON);
        return new DeleteStatement(ref, new SourceSpan(begin, previous().getTo()));
    }

    private FunctionDeclarationStatement functionDeclaration() {
        expect(Token.KeywordToken.FUNCTION);

        var name = expectWord().value();
        var begin = previous().getFrom();

        expect(Token.SimpleToken.BEGIN_PAREN);

        var arguments = new ArrayList<String>();
        while (peek().getToken() != Token.SimpleToken.END_PAREN) {
            expect(Token.SimpleToken.DOLLAR);
            var argument = expectWord().value();
            if (arguments.contains(argument)) {
                errors.add(new ParseException("Duplicate parameter name: '%s'".formatted(argument), previous().getPos()));
            }
            arguments.add(argument);
            if (peek().getToken() == Token.SimpleToken.COMMA) {
                next();
            } else {
                break;
            }
        }
        expect(Token.SimpleToken.END_PAREN);

        var body = blockStatement();
        return new FunctionDeclarationStatement(name, body, arguments, new SourceSpan(begin, previous().getTo()));
    }

    private Statement expressionStatement() {
        Expression expression;
        try {
            expression = expression();
        } catch (ParseException e) {
            errors.add(e);
            seek(Token.SimpleToken.SEMICOLON);
            return new ErrorStatement(e);
        }
        expect(Token.SimpleToken.SEMICOLON);
        return new ExpressionStatement(expression);
    }

    public Expression expression() {
        return expression(Precedence.ROOT);
    }

    public Expression expression(Precedence precedence) {
        var token = next();

        Expression left;
        try {
            left = PrefixParselet.getAndParse(this, token);
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

    private void seek(Token token) {
        while (hasNext() && peek().getToken() != token) next();
        expect(token);
    }

    public Token.WordToken expectWord() {
        var token = next().getToken();
        if (token instanceof Token.WordToken wordToken) return wordToken;
        return expectFail("word");
    }

    public void expect(Token token) {
        var found = next().getToken();
        if (found != token) expectFail(token.toString());
    }

    @Contract("_ -> fail")
    private <T> T expectFail(String expected) {
        throw new ParseException("Expected %s, but found %s".formatted(expected, previous().getToken()), previous().getPos());
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
