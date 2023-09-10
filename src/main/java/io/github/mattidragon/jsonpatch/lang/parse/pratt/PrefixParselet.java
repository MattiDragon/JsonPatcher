package io.github.mattidragon.jsonpatch.lang.parse.pratt;

import io.github.mattidragon.jsonpatch.lang.parse.PositionedToken;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatch.lang.parse.Token;
import io.github.mattidragon.jsonpatch.lang.ast.expression.*;
import io.github.mattidragon.jsonpatch.lang.ast.Value;
import io.github.mattidragon.jsonpatch.lang.parse.Parser;

import java.util.function.Function;
import java.util.function.Supplier;

public interface PrefixParselet<T extends PositionedToken<?>> {
    Expression parse(Parser parser, T token);

    PrefixParselet<PositionedToken.StringToken> STRING = literal(token -> new ValueExpression(new Value.StringValue(token.getToken().value()), token.getPos()));
    PrefixParselet<PositionedToken.NumberToken> NUMBER = literal(token -> new ValueExpression(new Value.NumberValue(token.getToken().value()), token.getPos()));
    PrefixParselet<PositionedToken.WordToken> IMPLICIT_ROOT = literal(token -> new PropertyAccessExpression(new RootExpression(token.getPos()), token.getToken().value(), token.getPos()));

    PrefixParselet<PositionedToken<?>> ROOT = constant(RootExpression::new);
    PrefixParselet<PositionedToken<?>> TRUE = constant(pos -> new ValueExpression(Value.BooleanValue.TRUE, pos));
    PrefixParselet<PositionedToken<?>> FALSE = constant(pos -> new ValueExpression(Value.BooleanValue.FALSE, pos));
    PrefixParselet<PositionedToken<?>> NULL = constant(pos -> new ValueExpression(Value.NullValue.NULL, pos));

    PrefixParselet<PositionedToken<?>> NOT = operator(UnaryExpression.Operator.NOT);
    PrefixParselet<PositionedToken<?>> BITWISE_NOT = operator(UnaryExpression.Operator.BITWISE_NOT);
    PrefixParselet<PositionedToken<?>> MINUS = operator(UnaryExpression.Operator.MINUS);

    static Expression getAndParse(Parser parser, PositionedToken<?> token) {
        if (token instanceof PositionedToken.StringToken stringToken) return STRING.parse(parser, stringToken);
        if (token instanceof PositionedToken.NumberToken numberToken) return NUMBER.parse(parser, numberToken);
        if (token instanceof PositionedToken.WordToken nameToken) return IMPLICIT_ROOT.parse(parser, nameToken);
        if (token.getToken() == Token.KeywordToken.THIS) return ROOT.parse(parser, token);
        if (token.getToken() == Token.KeywordToken.TRUE) return TRUE.parse(parser, token);
        if (token.getToken() == Token.KeywordToken.FALSE) return FALSE.parse(parser, token);
        if (token.getToken() == Token.KeywordToken.NULL) return NULL.parse(parser, token);
        if (token.getToken() == Token.SimpleToken.MINUS) return MINUS.parse(parser, token);
        if (token.getToken() == Token.SimpleToken.BANG) return NOT.parse(parser, token);
        if (token.getToken() == Token.SimpleToken.TILDE) return BITWISE_NOT.parse(parser, token);

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
