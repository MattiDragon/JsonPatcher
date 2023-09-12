package io.github.mattidragon.jsonpatch.lang.parse;

import io.github.mattidragon.jsonpatch.lang.PositionedException;
import io.github.mattidragon.jsonpatch.lang.runtime.Program;
import io.github.mattidragon.jsonpatch.lang.runtime.expression.Expression;
import io.github.mattidragon.jsonpatch.lang.runtime.expression.Reference;
import io.github.mattidragon.jsonpatch.lang.runtime.statement.*;
import io.github.mattidragon.jsonpatch.lang.parse.pratt.PostfixParselet;
import io.github.mattidragon.jsonpatch.lang.parse.pratt.Precedence;
import io.github.mattidragon.jsonpatch.lang.parse.pratt.PrefixParselet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<PositionedToken<?>> tokens;
    private int current = 0;

    public Parser(List<PositionedToken<?>> tokens) {
        this.tokens = tokens;
    }

    public Program program() {
        var statements = new ArrayList<Statement>();
        while (hasNext())
            statements.add(statement());
        return new Program(statements);
    }

    private Statement statement() {
        var token = peek();
        if (token.getToken() == Token.SimpleToken.BEGIN_CURLY) return blockStatement();
        if (token.getToken() == Token.KeywordToken.APPLY) return applyStatement();
        if (token.getToken() == Token.KeywordToken.IF) return ifStatement();
        if (token.getToken() == Token.KeywordToken.VAR) return variableStatement(true);
        if (token.getToken() == Token.KeywordToken.VAL) return variableStatement(false);
        if (token.getToken() == Token.KeywordToken.DELETE) return deleteStatement();
        return expressionStatement();
    }

    private BlockStatement blockStatement() {
        expect(Token.SimpleToken.BEGIN_CURLY);
        var beginPos = previous().getFrom();
        var statements = new ArrayList<Statement>();
        while (peek().getToken() != Token.SimpleToken.END_CURLY)
            statements.add(statement());
        expect(Token.SimpleToken.END_CURLY);
        var endPos = previous().getTo();
        return new BlockStatement(statements, new SourceSpan(beginPos, endPos));
    }

    private ApplyStatement applyStatement() {
        expect(Token.KeywordToken.APPLY);
        var beginPos = previous().getFrom();
        expect(Token.SimpleToken.BEGIN_PAREN);
        var root = expression();
        expect(Token.SimpleToken.END_PAREN);
        var action = statement();
        var endPos = previous().getTo();
        return new ApplyStatement(root, action, new SourceSpan(beginPos, endPos));
    }

    private IfStatement ifStatement() {
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

    private ExpressionStatement expressionStatement() {
        var expression = expression();
        expect(Token.SimpleToken.SEMICOLON);
        return new ExpressionStatement(expression);
    }

    public Expression expression() {
        return expression(Precedence.ROOT);
    }

    public Expression expression(Precedence precedence) {
        var token = next();

        var left = PrefixParselet.getAndParse(this, token);

        while (true) {
            var postfix = PostfixParselet.get(peek());
            if (postfix == null) break;
            if (precedence.ordinal() >= postfix.precedence().ordinal()) break;
            var postfixToken = next();
            left = postfix.parse(this, left, postfixToken);
        }

        return left;
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
        if (!hasNext()) throw new ParseException("Unexpected end of file", new SourceSpan(previous().getTo(), previous().getTo()));
        return tokens.get(current++);
    }

    public PositionedToken<?> previous() {
        if (current == 0) throw new IllegalStateException("No previous token (the parser is broken)");
        return tokens.get(current - 1);
    }

    public PositionedToken<?> peek() {
        if (!hasNext()) throw new ParseException("Unexpected end of file", new SourceSpan(previous().getTo(), previous().getTo()));
        return tokens.get(current);
    }

    public boolean hasNext() {
        return current < tokens.size();
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
