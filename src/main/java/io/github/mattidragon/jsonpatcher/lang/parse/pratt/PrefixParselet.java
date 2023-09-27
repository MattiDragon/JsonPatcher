package io.github.mattidragon.jsonpatcher.lang.parse.pratt;

import io.github.mattidragon.jsonpatcher.lang.parse.Parser;
import io.github.mattidragon.jsonpatcher.lang.parse.PositionedToken;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.parse.Token;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

public interface PrefixParselet<T extends PositionedToken<?>> {
    Expression parse(Parser parser, T token);

    PrefixParselet<PositionedToken.StringToken> STRING = literal(token -> new ValueExpression(new Value.StringValue(token.getToken().value()), token.getPos()));
    PrefixParselet<PositionedToken.NumberToken> NUMBER = literal(token -> new ValueExpression(new Value.NumberValue(token.getToken().value()), token.getPos()));

    PrefixParselet<PositionedToken.WordToken> IMPLICIT_ROOT = literal(token -> new ImplicitRootExpression(token.getToken().value(), token.getPos()));
    PrefixParselet<PositionedToken<?>> ROOT = constant(RootExpression::new);
    PrefixParselet<PositionedToken<?>> VARIABLE = (parser, token) -> {
        var name = parser.expectWord();
        return new VariableAccessExpression(name.value(), new SourceSpan(token.getFrom(), parser.previous().getTo()));
    };

    PrefixParselet<PositionedToken<?>> TRUE = constant(pos -> new ValueExpression(Value.BooleanValue.TRUE, pos));
    PrefixParselet<PositionedToken<?>> FALSE = constant(pos -> new ValueExpression(Value.BooleanValue.FALSE, pos));
    PrefixParselet<PositionedToken<?>> NULL = constant(pos -> new ValueExpression(Value.NullValue.NULL, pos));

    PrefixParselet<PositionedToken<?>> NOT = operator(UnaryExpression.Operator.NOT);
    PrefixParselet<PositionedToken<?>> BITWISE_NOT = operator(UnaryExpression.Operator.BITWISE_NOT);
    PrefixParselet<PositionedToken<?>> MINUS = operator(UnaryExpression.Operator.MINUS);

    PrefixParselet<PositionedToken<?>> ARRAY_INIT = (parser, token) -> {
        var children = new ArrayList<Expression>();
        while (parser.peek().getToken() != Token.SimpleToken.END_SQUARE) {
            children.add(parser.expression());

            if (parser.peek().getToken() == Token.SimpleToken.COMMA) {
                parser.next();
            } else {
                // If there is no comma we have to be at the last element.
                break;
            }
        }
        parser.expect(Token.SimpleToken.END_SQUARE);
        return new ArrayInitializerExpression(children, new SourceSpan(token.getFrom(), parser.previous().getTo()));
    };
    PrefixParselet<PositionedToken<?>> OBJECT_INIT = (parser, token) -> {
        var children = new HashMap<String, Expression>();
        while (parser.peek().getToken() != Token.SimpleToken.END_CURLY) {
            var key = parser.expectWord();
            parser.expect(Token.SimpleToken.COLON);
            children.put(key.value(), parser.expression());

            if (parser.peek().getToken() == Token.SimpleToken.COMMA) {
                parser.next();
            } else {
                // If there is no comma we have to be at the last element.
                break;
            }
        }
        parser.expect(Token.SimpleToken.END_CURLY);
        return new ObjectInitializerExpression(children, new SourceSpan(token.getFrom(), parser.previous().getTo()));
    };
    PrefixParselet<PositionedToken<?>> PARENTHESIS = (parser, token) -> {
        var expression = parser.expression();
        parser.expect(Token.SimpleToken.END_PAREN);
        return expression;
    };

    PrefixParselet<PositionedToken<?>> IMPORT = (parser, token) -> {
        parser.expect(Token.SimpleToken.BEGIN_PAREN);
        var name = parser.expectWord();
        parser.expect(Token.SimpleToken.END_PAREN);
        return new ImportExpression(name.value(), new SourceSpan(token.getFrom(), parser.previous().getTo()));
    };

    static Expression getAndParse(Parser parser, PositionedToken<?> token) {
        if (token instanceof PositionedToken.StringToken stringToken) return STRING.parse(parser, stringToken);
        if (token instanceof PositionedToken.NumberToken numberToken) return NUMBER.parse(parser, numberToken);
        if (token instanceof PositionedToken.WordToken nameToken) return IMPLICIT_ROOT.parse(parser, nameToken);
        if (token.getToken() == Token.KeywordToken.THIS) return ROOT.parse(parser, token);
        if (token.getToken() == Token.KeywordToken.TRUE) return TRUE.parse(parser, token);
        if (token.getToken() == Token.KeywordToken.FALSE) return FALSE.parse(parser, token);
        if (token.getToken() == Token.KeywordToken.NULL) return NULL.parse(parser, token);
        if (token.getToken() == Token.KeywordToken.IMPORT) return IMPORT.parse(parser, token);
        if (token.getToken() == Token.SimpleToken.DOLLAR) return VARIABLE.parse(parser, token);
        if (token.getToken() == Token.SimpleToken.MINUS) return MINUS.parse(parser, token);
        if (token.getToken() == Token.SimpleToken.BANG) return NOT.parse(parser, token);
        if (token.getToken() == Token.SimpleToken.TILDE) return BITWISE_NOT.parse(parser, token);
        if (token.getToken() == Token.SimpleToken.BEGIN_SQUARE) return ARRAY_INIT.parse(parser, token);
        if (token.getToken() == Token.SimpleToken.BEGIN_CURLY) return OBJECT_INIT.parse(parser, token);
        if (token.getToken() == Token.SimpleToken.BEGIN_PAREN) return PARENTHESIS.parse(parser, token);

        throw new Parser.ParseException("Unexpected token at start of expression: %s".formatted(token.getToken()), token.getPos());
    }

    private static <T extends PositionedToken<?>> PrefixParselet<T> operator(UnaryExpression.Operator operator) {
        return (parser, token) -> new UnaryExpression(parser.expression(Precedence.PREFIX), operator, token.getPos());
    }

    private static <T extends PositionedToken<?>> PrefixParselet<T> constant(Function<SourceSpan, Expression> operator) {
        return (parser, token) -> operator.apply(token.getPos());
    }

    private static <T extends PositionedToken<?>> PrefixParselet<T> literal(Function<T, Expression> operator) {
        return (parser, token) -> operator.apply(token);
    }
}
