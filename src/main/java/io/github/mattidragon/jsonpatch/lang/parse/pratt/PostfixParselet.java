package io.github.mattidragon.jsonpatch.lang.parse.pratt;

import io.github.mattidragon.jsonpatch.lang.parse.PositionedToken;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatch.lang.parse.Token;
import io.github.mattidragon.jsonpatch.lang.ast.expression.BinaryExpression;
import io.github.mattidragon.jsonpatch.lang.ast.expression.Expression;
import io.github.mattidragon.jsonpatch.lang.ast.expression.IndexExpression;
import io.github.mattidragon.jsonpatch.lang.ast.expression.PropertyAccessExpression;
import io.github.mattidragon.jsonpatch.lang.parse.Parser;

public interface PostfixParselet {
    Expression parse(Parser parser, Expression left, PositionedToken<?> token);
    Precedence precedence();

    record PropertyAccessParselet(Precedence precedence) implements PostfixParselet {
        @Override
        public Expression parse(Parser parser, Expression left, PositionedToken<?> token) {
            var name = parser.expectWord();
            return new PropertyAccessExpression(left, name.value(), new SourceSpan(token.getFrom(), parser.previous().getTo()));
        }
    }

    record IndexParselet(Precedence precedence) implements PostfixParselet {
        @Override
        public Expression parse(Parser parser, Expression left, PositionedToken<?> token) {
            var index = parser.expression();
            parser.expect(Token.SimpleToken.END_SQUARE);
            return new IndexExpression(left, index, new SourceSpan(token.getFrom(), parser.previous().getTo()));
        }
    }

    record BinaryOperationParselet(Precedence precedence, BinaryExpression.Operator operator) implements PostfixParselet {
        @Override
        public Expression parse(Parser parser, Expression left, PositionedToken<?> token) {
            var right = parser.expression(precedence);
            return new BinaryExpression(left, right, operator, token.getPos());
        }
    }

    static PostfixParselet get(PositionedToken<?> token) {
        if (token instanceof PositionedToken.SimpleToken simpleToken) {
            return switch (simpleToken.getToken()) {
                case DOT -> new PropertyAccessParselet(Precedence.POSTFIX);
                case BEGIN_SQUARE -> new IndexParselet(Precedence.POSTFIX);
                case PLUS -> new BinaryOperationParselet(Precedence.SUM, BinaryExpression.Operator.PLUS);
                case MINUS -> new BinaryOperationParselet(Precedence.SUM, BinaryExpression.Operator.MINUS);
                case STAR -> new BinaryOperationParselet(Precedence.PRODUCT, BinaryExpression.Operator.MULTIPLY);
                case SLASH -> new BinaryOperationParselet(Precedence.PRODUCT, BinaryExpression.Operator.DIVIDE);
                case PERCENT -> new BinaryOperationParselet(Precedence.PRODUCT, BinaryExpression.Operator.MODULO);
                default -> null;
            };
        }
        return null;
    }
}
