package io.github.mattidragon.jsonpatcher.lang.parse.parselet;

import io.github.mattidragon.jsonpatcher.lang.parse.Parser;
import io.github.mattidragon.jsonpatcher.lang.parse.PositionedToken;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.parse.Token;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.*;

import java.util.ArrayList;
import java.util.HashMap;

public class PrefixParser {
    private PrefixParser() {
    }

    private static Expression string(PositionedToken.StringToken token) {
        return new ValueExpression(new Value.StringValue(token.getToken().value()), token.getPos());
    }

    private static Expression number(PositionedToken.NumberToken token) {
        return new ValueExpression(new Value.NumberValue(token.getToken().value()), token.getPos());
    }

    private static Expression implicitRoot(PositionedToken.WordToken token) {
        return new ImplicitRootExpression(token.getToken().value(), token.getPos());
    }

    private static Expression root(PositionedToken<?> token) {
        return new RootExpression(token.getPos());
    }

    private static ValueExpression constant(PositionedToken<?> token, Value.Primitive value) {
        return new ValueExpression(value, token.getPos());
    }

    private static ImportExpression importExpression(Parser parser, PositionedToken<?> token) {
        parser.expect(Token.SimpleToken.BEGIN_PAREN);
        var name = parser.expectWord();
        parser.expect(Token.SimpleToken.END_PAREN);
        return new ImportExpression(name.value(), new SourceSpan(token.getFrom(), parser.previous().getTo()));
    }

    private static Expression variable(Parser parser, PositionedToken<?> token) {
        var name = parser.expectWord();
        return new VariableAccessExpression(name.value(), new SourceSpan(token.getFrom(), parser.previous().getTo()));
    }

    private static UnaryExpression unary(Parser parser, PositionedToken<?> token, UnaryExpression.Operator operator) {
        return new UnaryExpression(parser.expression(Precedence.PREFIX), operator, token.getPos());
    }

    private static Expression arrayInit(Parser parser, PositionedToken<?> token) {
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
    }

    private static Expression objectInit(Parser parser, PositionedToken<?> token) {
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
    }

    private static Expression parenthesis(Parser parser) {
        var expression = parser.expression();
        parser.expect(Token.SimpleToken.END_PAREN);
        return expression;
    }

    public static Expression parse(Parser parser, PositionedToken<?> token) {
        if (token instanceof PositionedToken.StringToken stringToken) return string(stringToken);
        if (token instanceof PositionedToken.NumberToken numberToken) return number(numberToken);
        if (token instanceof PositionedToken.WordToken nameToken) return implicitRoot(nameToken);
        if (token.getToken() == Token.KeywordToken.THIS) return root(token);
        if (token.getToken() == Token.KeywordToken.TRUE) return constant(token, Value.BooleanValue.TRUE);
        if (token.getToken() == Token.KeywordToken.FALSE) return constant(token, Value.BooleanValue.FALSE);
        if (token.getToken() == Token.KeywordToken.NULL) return constant(token, Value.NullValue.NULL);
        if (token.getToken() == Token.KeywordToken.IMPORT) return importExpression(parser, token);
        if (token.getToken() == Token.SimpleToken.DOLLAR) return variable(parser, token);
        if (token.getToken() == Token.SimpleToken.MINUS) return unary(parser, token, UnaryExpression.Operator.MINUS);
        if (token.getToken() == Token.SimpleToken.BANG) return unary(parser, token, UnaryExpression.Operator.NOT);
        if (token.getToken() == Token.SimpleToken.TILDE) return unary(parser, token, UnaryExpression.Operator.BITWISE_NOT);
        if (token.getToken() == Token.SimpleToken.BEGIN_SQUARE) return arrayInit(parser, token);
        if (token.getToken() == Token.SimpleToken.BEGIN_CURLY) return objectInit(parser, token);
        if (token.getToken() == Token.SimpleToken.BEGIN_PAREN) return parenthesis(parser);

        throw new Parser.ParseException("Unexpected token at start of expression: %s".formatted(token.getToken()), token.getPos());
    }
}
