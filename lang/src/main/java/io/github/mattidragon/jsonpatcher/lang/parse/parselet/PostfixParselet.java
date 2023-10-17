package io.github.mattidragon.jsonpatcher.lang.parse.parselet;

import io.github.mattidragon.jsonpatcher.lang.parse.Parser;
import io.github.mattidragon.jsonpatcher.lang.parse.PositionedToken;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.parse.Token;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.*;

import java.util.ArrayList;

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

    record UnaryModificationParselet(UnaryExpression.Operator operator) implements PostfixParselet {
        @Override
        public Expression parse(Parser parser, Expression left, PositionedToken<?> token) {
            if (!(left instanceof Reference ref)) throw new Parser.ParseException("Can't modify %s".formatted(left), token.getPos());
            return new UnaryModificationExpression(true, ref, operator, token.getPos());
        }

        @Override
        public Precedence precedence() {
            return Precedence.POSTFIX;
        }
    }

    record AssignmentParselet(BinaryExpression.Operator operator) implements PostfixParselet {
        @Override
        public Expression parse(Parser parser, Expression left, PositionedToken<?> token) {
            if (!(left instanceof Reference ref)) throw new Parser.ParseException("Can't assign to %s".formatted(left), token.getPos());
            var right = parser.expression(Precedence.ROOT);
            return new AssignmentExpression(ref, right, operator, token.getPos());
        }

        @Override
        public Precedence precedence() {
            return Precedence.ASSIGNMENT;
        }
    }

    record FunctionCallParselet() implements PostfixParselet {
        @Override
        public Expression parse(Parser parser, Expression left, PositionedToken<?> token) {
            var arguments = new ArrayList<Expression>();
            while (parser.peek().getToken() != Token.SimpleToken.END_PAREN) {
                arguments.add(parser.expression());
                if (parser.peek().getToken() == Token.SimpleToken.COMMA) {
                    parser.next();
                } else {
                    break;
                }
            }
            parser.expect(Token.SimpleToken.END_PAREN);

            return new FunctionCallExpression(left, arguments, new SourceSpan(token.getFrom(), parser.previous().getTo()));
        }

        @Override
        public Precedence precedence() {
            return Precedence.POSTFIX;
        }
    }

    record IsInstanceParselet() implements PostfixParselet {
        @Override
        public Expression parse(Parser parser, Expression left, PositionedToken<?> token) {
            var type = parser.next().getToken();
            if (type == Token.KeywordToken.NULL) {
                return new IsInstanceExpression(left, IsInstanceExpression.Type.NULL, token.getPos());
            }
            if (type instanceof Token.WordToken word) {
                switch (word.value()) {
                    case "number" -> {
                        return new IsInstanceExpression(left, IsInstanceExpression.Type.NUMBER, token.getPos());
                    }
                    case "string" -> {
                        return new IsInstanceExpression(left, IsInstanceExpression.Type.STRING, token.getPos());
                    }
                    case "boolean" -> {
                        return new IsInstanceExpression(left, IsInstanceExpression.Type.BOOLEAN, token.getPos());
                    }
                    case "array" -> {
                        return new IsInstanceExpression(left, IsInstanceExpression.Type.ARRAY, token.getPos());
                    }
                    case "object" -> {
                        return new IsInstanceExpression(left, IsInstanceExpression.Type.OBJECT, token.getPos());
                    }
                    case "function" -> {
                        return new IsInstanceExpression(left, IsInstanceExpression.Type.FUNCTION, token.getPos());
                    }
                }
            }
            throw new Parser.ParseException("Expected type name, got %s".formatted(type), token.getPos());
        }

        @Override
        public Precedence precedence() {
            return Precedence.COMPARISON;
        }
    }

    static PostfixParselet get(PositionedToken<?> token) {
        if (token.getToken() == Token.KeywordToken.IN) {
            return new BinaryOperationParselet(Precedence.COMPARISON, BinaryExpression.Operator.IN);
        }
        if (token.getToken() == Token.KeywordToken.IS) {
            return new IsInstanceParselet();
        }

        if (token instanceof PositionedToken.SimpleToken simpleToken) {
            return switch (simpleToken.getToken()) {
                case DOT -> new PropertyAccessParselet();
                case BEGIN_SQUARE -> new IndexParselet();
                case BEGIN_PAREN -> new FunctionCallParselet();

                case PLUS -> new BinaryOperationParselet(Precedence.SUM, BinaryExpression.Operator.PLUS);
                case MINUS -> new BinaryOperationParselet(Precedence.SUM, BinaryExpression.Operator.MINUS);
                case STAR -> new BinaryOperationParselet(Precedence.PRODUCT, BinaryExpression.Operator.MULTIPLY);
                case SLASH -> new BinaryOperationParselet(Precedence.PRODUCT, BinaryExpression.Operator.DIVIDE);
                case PERCENT -> new BinaryOperationParselet(Precedence.PRODUCT, BinaryExpression.Operator.MODULO);
                case DOUBLE_STAR -> new BinaryOperationParselet(Precedence.EXPONENT, BinaryExpression.Operator.EXPONENT);
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

                case DOUBLE_MINUS -> new UnaryModificationParselet(UnaryExpression.Operator.DECREMENT);
                case DOUBLE_PLUS -> new UnaryModificationParselet(UnaryExpression.Operator.INCREMENT);
                case DOUBLE_BANG -> new UnaryModificationParselet(UnaryExpression.Operator.NOT);

                case ASSIGN -> new AssignmentParselet(BinaryExpression.Operator.ASSIGN);
                case PLUS_ASSIGN -> new AssignmentParselet(BinaryExpression.Operator.PLUS);
                case MINUS_ASSIGN -> new AssignmentParselet(BinaryExpression.Operator.MINUS);
                case STAR_ASSIGN -> new AssignmentParselet(BinaryExpression.Operator.MULTIPLY);
                case SLASH_ASSIGN -> new AssignmentParselet(BinaryExpression.Operator.DIVIDE);
                case PERCENT_ASSIGN -> new AssignmentParselet(BinaryExpression.Operator.MODULO);
                case OR_ASSIGN -> new AssignmentParselet(BinaryExpression.Operator.OR);
                case XOR_ASSIGN -> new AssignmentParselet(BinaryExpression.Operator.XOR);
                case AND_ASSIGN -> new AssignmentParselet(BinaryExpression.Operator.AND);

                default -> null;
            };
        }
        return null;
    }
}
