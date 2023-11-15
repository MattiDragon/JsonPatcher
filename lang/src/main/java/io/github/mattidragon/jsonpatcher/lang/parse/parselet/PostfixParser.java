package io.github.mattidragon.jsonpatcher.lang.parse.parselet;

import io.github.mattidragon.jsonpatcher.lang.parse.Parser;
import io.github.mattidragon.jsonpatcher.lang.parse.PositionedToken;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.parse.Token;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.*;

import java.util.ArrayList;

// Consider reworking this to use a registration or lookup system instead of a switch
public class PostfixParser {
    private PostfixParser() {}

    private static Expression parsePropertyAccess(Parser parser, Expression left, PositionedToken<?> token) {
        var name = parser.expectWord();
        return new PropertyAccessExpression(left, name.value(), new SourceSpan(token.getFrom(), parser.previous().getTo()));
    }

    private static Expression parseIndexAccess(Parser parser, Expression left, PositionedToken<?> token) {
        var index = parser.expression();
        parser.expect(Token.SimpleToken.END_SQUARE);
        return new IndexExpression(left, index, new SourceSpan(token.getFrom(), parser.previous().getTo()));
    }

    private static Expression parseShortedBinaryOperation(Parser parser, Expression left, PositionedToken<?> token, ShortedBinaryExpression.Operator operator, Precedence precedence) {
        var right = parser.expression(precedence);
        return new ShortedBinaryExpression(left, right, operator, token.getPos());
    }

    private static Expression parseBinaryOperation(Parser parser, Expression left, PositionedToken<?> token, BinaryExpression.Operator operator, Precedence precedence) {
        var right = parser.expression(precedence);
        return new BinaryExpression(left, right, operator, token.getPos());
    }

    private static Expression parseUnaryModification(Expression left, PositionedToken<?> token, UnaryExpression.Operator operator) {
        if (!(left instanceof Reference ref)) throw new Parser.ParseException("Can't modify %s".formatted(left), token.getPos());
        return new UnaryModificationExpression(true, ref, operator, token.getPos());
    }

    private static Expression parseAssignment(Parser parser, Expression left, PositionedToken<?> token, BinaryExpression.Operator operator) {
        if (!(left instanceof Reference ref)) throw new Parser.ParseException("Can't assign to %s".formatted(left), token.getPos());
        var right = parser.expression(Precedence.ROOT);
        return new AssignmentExpression(ref, right, operator, token.getPos());
    }

    private static Expression parseFunctionCall(Parser parser, Expression left, PositionedToken<?> token) {
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

    private static Expression parseIsInstance(Parser parser, Expression left, PositionedToken<?> token) {
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

    public static Expression get(Parser parser, Precedence precedence, Expression left) {
        var token = parser.peek();
        if (token instanceof PositionedToken.KeywordToken keywordToken && precedence.ordinal() <= Precedence.COMPARISON.ordinal()) {
            if (keywordToken.getToken() == Token.KeywordToken.IS) {
                return parseIsInstance(parser, left, parser.next());
            }
            if (keywordToken.getToken() == Token.KeywordToken.IN) {
                return parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.IN, Precedence.COMPARISON);
            }
        }

        if (!(token.getToken() instanceof Token.SimpleToken simpleToken)) return null;

        // abuse fallthrough to check precedence levels in order
        switch (precedence) {
            case ROOT:
            case ASSIGNMENT: {
                var expression = switch (simpleToken) {
                    case ASSIGN -> parseAssignment(parser, left, parser.next(), BinaryExpression.Operator.ASSIGN);
                    case PLUS_ASSIGN -> parseAssignment(parser, left, parser.next(), BinaryExpression.Operator.PLUS);
                    case MINUS_ASSIGN -> parseAssignment(parser, left, parser.next(), BinaryExpression.Operator.MINUS);
                    case STAR_ASSIGN -> parseAssignment(parser, left, parser.next(), BinaryExpression.Operator.MULTIPLY);
                    case SLASH_ASSIGN -> parseAssignment(parser, left, parser.next(), BinaryExpression.Operator.DIVIDE);
                    case PERCENT_ASSIGN -> parseAssignment(parser, left, parser.next(), BinaryExpression.Operator.MODULO);
                    case OR_ASSIGN -> parseAssignment(parser, left, parser.next(), BinaryExpression.Operator.OR);
                    case XOR_ASSIGN -> parseAssignment(parser, left, parser.next(), BinaryExpression.Operator.XOR);
                    case AND_ASSIGN -> parseAssignment(parser, left, parser.next(), BinaryExpression.Operator.AND);
                    default -> null;
                };
                if (expression != null) return expression;
            }
            case OR:
                if (token.getToken() == Token.SimpleToken.DOUBLE_OR) {
                    return parseShortedBinaryOperation(parser, left, parser.next(), ShortedBinaryExpression.Operator.OR, Precedence.OR);
                }
            case AND:
                if (token.getToken() == Token.SimpleToken.DOUBLE_AND) {
                    return parseShortedBinaryOperation(parser, left, parser.next(), ShortedBinaryExpression.Operator.AND, Precedence.AND);
                }
            case BITWISE_OR:
                if (token.getToken() == Token.SimpleToken.OR) {
                    return parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.OR, Precedence.BITWISE_OR);
                }
            case BITWISE_XOR:
                if (token.getToken() == Token.SimpleToken.XOR) {
                    return parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.XOR, Precedence.BITWISE_XOR);
                }
            case BITWISE_AND:
                if (token.getToken() == Token.SimpleToken.AND) {
                    return parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.AND, Precedence.BITWISE_AND);
                }
            case EQUALITY: {
                var expression = switch (simpleToken) {
                    case EQUALS -> parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.EQUALS, Precedence.EQUALITY);
                    case NOT_EQUALS -> parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.NOT_EQUALS, Precedence.EQUALITY);
                    default -> null;
                };
                if (expression != null) return expression;
            }
            case COMPARISON: {
                var expression = switch (simpleToken) {
                    case LESS_THAN -> parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.LESS_THAN, Precedence.COMPARISON);
                    case LESS_THAN_EQUAL -> parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.LESS_THAN_EQUAL, Precedence.COMPARISON);
                    case GREATER_THAN -> parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.GREATER_THAN, Precedence.COMPARISON);
                    case GREATER_THAN_EQUAL -> parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.GREATER_THAN_EQUAL, Precedence.COMPARISON);
                    default -> null;
                };
                if (expression != null) return expression;
            }
            case BIT_SHIFT:
            case SUM: {
                var expression = switch (simpleToken) {
                    case PLUS -> parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.PLUS, Precedence.SUM);
                    case MINUS -> parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.MINUS, Precedence.SUM);
                    default -> null;
                };
                if (expression != null) return expression;
            }
            case PRODUCT: {
                var expression = switch (simpleToken) {
                    case STAR -> parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.MULTIPLY, Precedence.PRODUCT);
                    case SLASH -> parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.DIVIDE, Precedence.PRODUCT);
                    case PERCENT -> parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.MODULO, Precedence.PRODUCT);
                    default -> null;
                };
                if (expression != null) return expression;
            }
            case EXPONENT: {
                if (simpleToken == Token.SimpleToken.DOUBLE_STAR) {
                    return parseBinaryOperation(parser, left, parser.next(), BinaryExpression.Operator.EXPONENT, Precedence.EXPONENT);
                }
            }
            case PREFIX:
            case POSTFIX: {
                var expression = switch (simpleToken) {
                    case DOT -> parsePropertyAccess(parser, left, parser.next());
                    case BEGIN_SQUARE -> parseIndexAccess(parser, left, parser.next());
                    case BEGIN_PAREN -> parseFunctionCall(parser, left, parser.next());
                    case DOUBLE_MINUS -> parseUnaryModification(left, parser.next(), UnaryExpression.Operator.DECREMENT);
                    case DOUBLE_PLUS -> parseUnaryModification(left, parser.next(), UnaryExpression.Operator.INCREMENT);
                    case DOUBLE_BANG -> parseUnaryModification(left, parser.next(), UnaryExpression.Operator.NOT);
                    default -> null;
                };
                if (expression != null) return expression;
            }
            default:
                return null;
        }
    }
}
