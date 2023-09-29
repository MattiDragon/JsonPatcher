package io.github.mattidragon.jsonpatcher.lang.parse.parselet;

import io.github.mattidragon.jsonpatcher.lang.parse.Parser;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.parse.Token;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Expression;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Reference;
import io.github.mattidragon.jsonpatcher.lang.runtime.statement.*;

import java.util.ArrayList;
import java.util.Optional;

public class StatementParser {
    private StatementParser() {
    }

    private static Statement blockStatement(Parser parser) {
        parser.expect(Token.SimpleToken.BEGIN_CURLY);
        var beginPos = parser.previous().getFrom();
        var statements = new ArrayList<Statement>();
        try {
            while (parser.peek().getToken() != Token.SimpleToken.END_CURLY)
                statements.add(parse(parser));
        } catch (Parser.ParseException e) {
            parser.addError(e);
            parser.seek(Token.SimpleToken.END_CURLY);
            return new ErrorStatement(e);
        }
        parser.expect(Token.SimpleToken.END_CURLY);
        var endPos = parser.previous().getTo();
        return new BlockStatement(statements, new SourceSpan(beginPos, endPos));
    }

    private static Statement applyStatement(Parser parser) {
        parser.expect(Token.KeywordToken.APPLY);
        var beginPos = parser.previous().getFrom();
        parser.expect(Token.SimpleToken.BEGIN_PAREN);
        var root = parser.expression();
        parser.expect(Token.SimpleToken.END_PAREN);
        var action = parse(parser);
        var endPos = parser.previous().getTo();
        return new ApplyStatement(root, action, new SourceSpan(beginPos, endPos));
    }

    private static Statement ifStatement(Parser parser) {
        parser.expect(Token.KeywordToken.IF);
        var beginPos = parser.previous().getFrom();
        parser.expect(Token.SimpleToken.BEGIN_PAREN);
        var condition = parser.expression();
        parser.expect(Token.SimpleToken.END_PAREN);
        var action = parse(parser);
        Statement elseAction = null;
        if (parser.hasNext(Token.KeywordToken.ELSE)) {
            parser.next();
            elseAction = parse(parser);
        }
        var endPos = parser.previous().getTo();
        return new IfStatement(condition, action, elseAction, new SourceSpan(beginPos, endPos));
    }

    private static Statement variableStatement(Parser parser, boolean mutable) {
        var begin = parser.next().getFrom();
        parser.expect(Token.SimpleToken.DOLLAR);
        var name = parser.expectWord();
        parser.expect(Token.SimpleToken.ASSIGN);
        var initializer = parser.expression();
        parser.expect(Token.SimpleToken.SEMICOLON);
        return new VariableCreationStatement(name.value(), initializer, mutable, new SourceSpan(begin, parser.previous().getTo()));
    }

    private static Statement deleteStatement(Parser parser) {
        var begin = parser.next().getFrom();
        var expression = parser.expression();
        if (!(expression instanceof Reference ref)) throw new Parser.ParseException("Can't delete to %s".formatted(expression), expression.getPos());
        parser.expect(Token.SimpleToken.SEMICOLON);
        return new DeleteStatement(ref, new SourceSpan(begin, parser.previous().getTo()));
    }

    private static Statement returnStatement(Parser parser) {
        var begin = parser.next().getFrom();
        Optional<Expression> value;
        if (parser.peek().getToken() == Token.SimpleToken.SEMICOLON) {
            value = Optional.empty();
        } else {
            value = Optional.of(parser.expression());
        }
        parser.expect(Token.SimpleToken.SEMICOLON);
        return new ReturnStatement(value, new SourceSpan(begin, parser.previous().getTo()));
    }

    private static FunctionDeclarationStatement functionDeclaration(Parser parser) {
        parser.expect(Token.KeywordToken.FUNCTION);

        var name = parser.expectWord().value();
        var begin = parser.previous().getFrom();

        parser.expect(Token.SimpleToken.BEGIN_PAREN);

        var arguments = new ArrayList<String>();
        while (parser.peek().getToken() != Token.SimpleToken.END_PAREN) {
            parser.expect(Token.SimpleToken.DOLLAR);
            var argument = parser.expectWord().value();
            if (arguments.contains(argument)) {
                parser.addError(new Parser.ParseException("Duplicate parameter name: '%s'".formatted(argument), parser.previous().getPos()));
            }
            arguments.add(argument);
            if (parser.peek().getToken() == Token.SimpleToken.COMMA) {
                parser.next();
            } else {
                break;
            }
        }
        parser.expect(Token.SimpleToken.END_PAREN);

        var body = blockStatement(parser);
        return new FunctionDeclarationStatement(name, body, arguments, new SourceSpan(begin, parser.previous().getTo()));
    }

    private static Statement expressionStatement(Parser parser) {
        Expression expression;
        try {
            expression = parser.expression();
        } catch (Parser.ParseException e) {
            parser.addError(e);
            parser.seek(Token.SimpleToken.SEMICOLON);
            return new ErrorStatement(e);
        }
        parser.expect(Token.SimpleToken.SEMICOLON);
        return new ExpressionStatement(expression);
    }

    public static Statement parse(Parser parser) {
        var token = parser.peek();
        if (token.getToken() == Token.SimpleToken.BEGIN_CURLY) return blockStatement(parser);
        if (token.getToken() == Token.KeywordToken.APPLY) return applyStatement(parser);
        if (token.getToken() == Token.KeywordToken.IF) return ifStatement(parser);
        if (token.getToken() == Token.KeywordToken.VAR) return variableStatement(parser,true);
        if (token.getToken() == Token.KeywordToken.VAL) return variableStatement(parser,false);
        if (token.getToken() == Token.KeywordToken.DELETE) return deleteStatement(parser);
        if (token.getToken() == Token.KeywordToken.RETURN) return returnStatement(parser);
        if (token.getToken() == Token.KeywordToken.FUNCTION) return functionDeclaration(parser);
        return expressionStatement(parser);
    }
}
