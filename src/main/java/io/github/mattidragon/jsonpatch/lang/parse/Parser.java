package io.github.mattidragon.jsonpatch.lang.parse;

import io.github.mattidragon.jsonpatch.lang.ast.Program;
import io.github.mattidragon.jsonpatch.lang.ast.expression.Expression;
import io.github.mattidragon.jsonpatch.lang.ast.Statement;
import io.github.mattidragon.jsonpatch.lang.ast.expression.Reference;
import io.github.mattidragon.jsonpatch.lang.parse.pratt.PostfixParselet;
import io.github.mattidragon.jsonpatch.lang.parse.pratt.Precedence;
import io.github.mattidragon.jsonpatch.lang.parse.pratt.PrefixParselet;
import org.jetbrains.annotations.Contract;

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
        return assignmentStatement();
    }

    private Statement.Block blockStatement() {
        expect(Token.SimpleToken.BEGIN_CURLY);
        var beginPos = previous().getFrom();
        var statements = new ArrayList<Statement>();
        while (peek().getToken() != Token.SimpleToken.END_CURLY)
            statements.add(statement());
        expect(Token.SimpleToken.END_CURLY);
        var endPos = previous().getTo();
        return new Statement.Block(statements, new SourceSpan(beginPos, endPos));
    }

    private Statement.Apply applyStatement() {
        expect(Token.KeywordToken.APPLY);
        var beginPos = previous().getFrom();
        expect(Token.SimpleToken.BEGIN_PAREN);
        var root = expression();
        expect(Token.SimpleToken.END_PAREN);
        var action = statement();
        var endPos = previous().getTo();
        return new Statement.Apply(root, action, new SourceSpan(beginPos, endPos));
    }

    private Statement.Assignment assignmentStatement() {
        var target = expression();
        if (!(target instanceof Reference ref)) throw new ParseException("Can't assign to %s".formatted(target), target.getPos());
        expect(Token.SimpleToken.EQUALS);
        var pos = previous().getPos();
        var val = expression();
        expect(Token.SimpleToken.SEMICOLON);
        return new Statement.Assignment(ref, val, pos);
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
        if (!hasNext()) throw new IllegalStateException("Reached end of file without EOF token");
        return tokens.get(current++);
    }

    public PositionedToken<?> previous() {
        if (current == 0) throw new IllegalStateException("No previous token");
        return tokens.get(current - 1);
    }

    public PositionedToken<?> peek() {
        if (!hasNext()) throw new IllegalStateException("Unexpected end of file");
        return tokens.get(current);
    }

    public boolean hasNext() {
        return current < tokens.size();
    }

    public static class ParseException extends RuntimeException {
        public final SourceSpan pos;

        public ParseException(String message, SourceSpan pos) {
            super(message);
            this.pos = pos;
        }
    }
}
