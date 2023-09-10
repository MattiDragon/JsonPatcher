package io.github.mattidragon.jsonpatch.lang.parse.pratt;

import io.github.mattidragon.jsonpatch.lang.ast.expression.*;
import io.github.mattidragon.jsonpatch.lang.parse.PositionedToken;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatch.lang.parse.Token;
import io.github.mattidragon.jsonpatch.lang.parse.Parser;

public interface PostfixParselet {
    Expression parse(Parser parser, Expression left, PositionedToken<?> token);
    Precedence precedence();

    record PropertyAccessParselet() implements PostfixParselet {
        @Override
        public Expression parse(Parser parser, Expression left, PositionedToken<?> token) {
            var name = parser.expectWord();
            return new PropertyAccessExpression(left, name.value(), new SourceSpan(token.getFrom(), parser.previous().getTo()));
        }

        @Override
        public Precedence precedence() {
            return Precedence.POSTFIX;
        }
    }

    record IndexParselet() implements PostfixParselet {
        @Override
        public Expression parse(Parser parser, Expression left, PositionedToken<?> token) {
            var index = parser.expression();
            parser.expect(Token.SimpleToken.END_SQUARE);
            return new IndexExpression(left, index, new SourceSpan(token.getFrom(), parser.previous().getTo()));
        }

        @Override
        public Precedence precedence() {
            return Precedence.POSTFIX;
        }
    }

    record ShortedBinaryOperationParselet(Precedence precedence, ShortedBinaryExpression.Operator operator) implements PostfixParselet {
        @Override
        public Expression parse(Parser parser, Expression left, PositionedToken<?> token) {
            var right = parser.expression(precedence);
            return new ShortedBinaryExpression(left, right, operator, token.getPos());
        }
    }

    record BinaryOperationParselet(Precedence precedence, BinaryExpression.Operator operator) implements PostfixParselet {
        @Override
        public Expression parse(Parser parser, Expression left, PositionedToken<?> token) {
            var right = parser.expression(precedence);
            return new BinaryExpression(left, right, operator, token.getPos());
        }
    }

    record AssignmentParselet(BinaryExpression.Operator operator) implements PostfixParselet {
        @Override
        public Expression parse(Parser parser, Expression left, PositionedToken<?> token) {
            if (!(left instanceof Reference ref)) throw new Parser.ParseException("Can't assign to %s".formatted(left), left.getPos());
            var right = parser.expression(Precedence.ROOT);
            return new AssignmentExpression(ref, right, operator, token.getPos());
        }

        @Override
        public Precedence precedence() {
            return Precedence.ASSIGNMENT;
        }
    }

    static PostfixParselet get(PositionedToken<?> token) {
        if (token instanceof PositionedToken.SimpleToken simpleToken) {
            return switch (simpleToken.getToken()) {
                case DOT -> new PropertyAccessParselet();
                case BEGIN_SQUARE -> new IndexParselet();

                case PLUS -> new BinaryOperationParselet(Precedence.SUM, BinaryExpression.Operator.PLUS);
                case MINUS -> new BinaryOperationParselet(Precedence.SUM, BinaryExpression.Operator.MINUS);
                case STAR -> new BinaryOperationParselet(Precedence.PRODUCT, BinaryExpression.Operator.MULTIPLY);
                case SLASH -> new BinaryOperationParselet(Precedence.PRODUCT, BinaryExpression.Operator.DIVIDE);
                case PERCENT -> new BinaryOperationParselet(Precedence.PRODUCT, BinaryExpression.Operator.MODULO);
                case OR -> new BinaryOperationParselet(Precedence.BITWISE_OR, BinaryExpression.Operator.OR);
                case AND -> new BinaryOperationParselet(Precedence.BITWISE_AND, BinaryExpression.Operator.AND);
                case XOR -> new BinaryOperationParselet(Precedence.BITWISE_XOR, BinaryExpression.Operator.XOR);
                case DOUBLE_OR -> new ShortedBinaryOperationParselet(Precedence.OR, ShortedBinaryExpression.Operator.OR);
                case DOUBLE_AND -> new ShortedBinaryOperationParselet(Precedence.AND, ShortedBinaryExpression.Operator.AND);

                case EQUALS -> new BinaryOperationParselet(Precedence.EQUALITY, BinaryExpression.Operator.EQUALS);
                case NOT_EQUALS -> new BinaryOperationParselet(Precedence.EQUALITY, BinaryExpression.Operator.NOT_EQUALS);
                case LESS_THAN -> new BinaryOperationParselet(Precedence.COMPARISON, BinaryExpression.Operator.LESS_THAN);
                case LESS_THAN_EQUAL -> new BinaryOperationParselet(Precedence.COMPARISON, BinaryExpression.Operator.LESS_THAN_EQUAL);
                case GREATER_THAN -> new BinaryOperationParselet(Precedence.COMPARISON, BinaryExpression.Operator.GREATER_THAN);
                case GREATER_THAN_EQUAL -> new BinaryOperationParselet(Precedence.COMPARISON, BinaryExpression.Operator.GREATER_THAN_EQUAL);

                case ASSIGN -> new AssignmentParselet(BinaryExpression.Operator.ASSIGN);
                case PLUS_ASSIGN -> new AssignmentParselet(BinaryExpression.Operator.PLUS);
                case MINUS_ASSIGN -> new AssignmentParselet(BinaryExpression.Operator.MINUS);
                case STAR_ASSIGN -> new AssignmentParselet(BinaryExpression.Operator.MULTIPLY);
                case SLASH_ASSIGN -> new AssignmentParselet(BinaryExpression.Operator.DIVIDE);
                case PERCENT_ASSIGN -> new AssignmentParselet(BinaryExpression.Operator.MODULO);

                default -> null;
            };
        }
        return null;
    }
}
