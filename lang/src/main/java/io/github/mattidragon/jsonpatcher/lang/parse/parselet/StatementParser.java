package io.github.mattidragon.jsonpatcher.lang.parse.parselet;

import io.github.mattidragon.jsonpatcher.lang.parse.Parser;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Expression;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.FunctionExpression;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Reference;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.ValueExpression;
import io.github.mattidragon.jsonpatcher.lang.runtime.statement.*;

import java.util.ArrayList;
import java.util.Optional;

import static io.github.mattidragon.jsonpatcher.lang.parse.Token.KeywordToken;
import static io.github.mattidragon.jsonpatcher.lang.parse.Token.SimpleToken;

public class StatementParser {
    private StatementParser() {
    }

    static Statement blockStatement(Parser parser) {
        parser.expect(SimpleToken.BEGIN_CURLY);
        var beginPos = parser.previous().getFrom();
        var statements = new ArrayList<Statement>();
        try {
            while (parser.peek().getToken() != SimpleToken.END_CURLY)
                statements.add(parse(parser));
        } catch (Parser.ParseException e) {
            parser.addError(e);
            parser.seek(SimpleToken.END_CURLY);
            return new ErrorStatement(e);
        }
        parser.expect(SimpleToken.END_CURLY);
        var endPos = parser.previous().getTo();
        return new BlockStatement(statements, new SourceSpan(beginPos, endPos));
    }

    private static Statement applyStatement(Parser parser) {
        parser.expect(KeywordToken.APPLY);
        var beginPos = parser.previous().getFrom();
        parser.expect(SimpleToken.BEGIN_PAREN);
        var root = parser.expression();
        parser.expect(SimpleToken.END_PAREN);
        var action = parse(parser);
        var endPos = parser.previous().getTo();
        return new ApplyStatement(root, action, new SourceSpan(beginPos, endPos));
    }

    private static Statement ifStatement(Parser parser) {
        parser.expect(KeywordToken.IF);
        var beginPos = parser.previous().getFrom();
        parser.expect(SimpleToken.BEGIN_PAREN);
        var condition = parser.expression();
        parser.expect(SimpleToken.END_PAREN);
        var action = parse(parser);
        Statement elseAction = null;
        if (parser.hasNext(KeywordToken.ELSE)) {
            parser.next();
            elseAction = parse(parser);
        }
        var endPos = parser.previous().getTo();
        return new IfStatement(condition, action, elseAction, new SourceSpan(beginPos, endPos));
    }

    private static Statement variableStatement(Parser parser, boolean mutable) {
        var begin = parser.next().getFrom();
        var name = parser.expectWord();
        parser.expect(SimpleToken.ASSIGN);
        var initializer = parser.expression();
        parser.expect(SimpleToken.SEMICOLON);
        return new VariableCreationStatement(name.value(), initializer, mutable, new SourceSpan(begin, parser.previous().getTo()));
    }

    private static Statement deleteStatement(Parser parser) {
        var begin = parser.next().getFrom();
        var expression = parser.expression();
        if (!(expression instanceof Reference ref)) throw new Parser.ParseException("Can't delete to %s".formatted(expression), expression.getPos());
        parser.expect(SimpleToken.SEMICOLON);
        return new DeleteStatement(ref, new SourceSpan(begin, parser.previous().getTo()));
    }

    private static Statement returnStatement(Parser parser) {
        var begin = parser.next().getFrom();
        Optional<Expression> value;
        if (parser.peek().getToken() == SimpleToken.SEMICOLON) {
            value = Optional.empty();
        } else {
            value = Optional.of(parser.expression());
        }
        parser.expect(SimpleToken.SEMICOLON);
        return new ReturnStatement(value, new SourceSpan(begin, parser.previous().getTo()));
    }

    private static FunctionDeclarationStatement functionDeclaration(Parser parser) {
        parser.expect(KeywordToken.FUNCTION);

        var name = parser.expectWord().value();
        var begin = parser.previous().getFrom();

        parser.expect(SimpleToken.BEGIN_PAREN);
        var arguments = PrefixParser.parseArgumentList(parser);
        var body = blockStatement(parser);
        var expression = new FunctionExpression(body, arguments, new SourceSpan(begin, parser.previous().getTo()));

        return new FunctionDeclarationStatement(name, expression);
    }

    private static Statement expressionStatement(Parser parser) {
        Expression expression;
        try {
            expression = parser.expression();
        } catch (Parser.ParseException e) {
            parser.addError(e);
            parser.seek(SimpleToken.SEMICOLON);
            return new ErrorStatement(e);
        }
        parser.expect(SimpleToken.SEMICOLON);
        return new ExpressionStatement(expression);
    }

    private static Statement whileLoop(Parser parser) {
        parser.expect(KeywordToken.WHILE);
        var from = parser.previous().getFrom();
        parser.expect(SimpleToken.BEGIN_PAREN);
        var condition = parser.expression();
        parser.expect(SimpleToken.END_PAREN);
        var to = parser.previous().getTo();
        var body = parse(parser);

        return new WhileLoopStatement(condition, body, new SourceSpan(from, to));
    }

    private static Statement forLoop(Parser parser) {
        parser.expect(KeywordToken.FOR);
        var from = parser.previous().getFrom();
        parser.expect(SimpleToken.BEGIN_PAREN);

        Statement initializer;
        if (parser.hasNext(SimpleToken.SEMICOLON)) {
            parser.next();
            initializer = new UnnecessarySemicolonStatement(parser.previous().getPos());
        } else if (parser.hasNext(KeywordToken.VAR) || parser.hasNext(KeywordToken.VAL)) {
            initializer = variableStatement(parser, parser.peek().getToken() == KeywordToken.VAR);
        } else {
            initializer = expressionStatement(parser);
        }
        // Semicolon is handled by the variable or expression statement

        Expression condition;
        if (parser.hasNext(SimpleToken.SEMICOLON)) {
            condition = new ValueExpression(Value.BooleanValue.TRUE, parser.peek().getPos());
        } else {
            condition = parser.expression();
        }
        parser.expect(SimpleToken.SEMICOLON);

        Statement incrementer;
        if (parser.hasNext(SimpleToken.SEMICOLON)) {
            parser.next();
            incrementer = new UnnecessarySemicolonStatement(parser.previous().getPos());
        } else {
            incrementer = new ExpressionStatement(parser.expression());
        }

        parser.expect(SimpleToken.END_PAREN);
        var to = parser.previous().getTo();
        var body = parse(parser);
        return new ForLoopStatement(initializer, condition, incrementer, body, new SourceSpan(from, to));
    }

    private static Statement forEachLoop(Parser parser) {
        parser.expect(KeywordToken.FOREACH);
        var from = parser.previous().getFrom();
        parser.expect(SimpleToken.BEGIN_PAREN);
        var name = parser.expectWord();
        parser.expect(KeywordToken.IN);
        var expression = parser.expression();
        parser.expect(SimpleToken.END_PAREN);
        var to = parser.previous().getTo();
        var body = parse(parser);

        return new ForEachLoopStatement(expression, name.value(), body, new SourceSpan(from, to));
    }

    private static Statement breakStatement(Parser parser) {
        parser.expect(KeywordToken.BREAK);
        var from = parser.previous().getFrom();
        parser.expect(SimpleToken.SEMICOLON);
        return new BreakStatement(new SourceSpan(from, parser.previous().getTo()));
    }

    private static Statement continueStatement(Parser parser) {
        parser.expect(KeywordToken.CONTINUE);
        var from = parser.previous().getFrom();
        parser.expect(SimpleToken.SEMICOLON);
        return new ContinueStatement(new SourceSpan(from, parser.previous().getTo()));
    }

    private static Statement importStatement(Parser parser) {
        parser.expect(KeywordToken.IMPORT);
        var from = parser.previous().getFrom();
        var libraryName = parser.expectString().value();
        if (parser.hasNext(KeywordToken.AS)) {
            parser.next();
            var variableName = parser.expectWord().value();
            parser.expect(SimpleToken.SEMICOLON);
            return new ImportStatement(libraryName, variableName, new SourceSpan(from, parser.previous().getTo()));
        } else {
            parser.expect(SimpleToken.SEMICOLON);
            return new ImportStatement(libraryName, libraryName, new SourceSpan(from, parser.previous().getTo()));
        }
    }

    public static Statement parse(Parser parser) {
        var token = parser.peek();
        if (token.getToken() == SimpleToken.BEGIN_CURLY) return blockStatement(parser);
        if (token.getToken() == SimpleToken.SEMICOLON) {
            parser.next();
            return new UnnecessarySemicolonStatement(token.getPos());
        }
        if (token.getToken() == KeywordToken.APPLY) return applyStatement(parser);
        if (token.getToken() == KeywordToken.IF) return ifStatement(parser);
        if (token.getToken() == KeywordToken.VAR) return variableStatement(parser,true);
        if (token.getToken() == KeywordToken.VAL) return variableStatement(parser,false);
        if (token.getToken() == KeywordToken.DELETE) return deleteStatement(parser);
        if (token.getToken() == KeywordToken.RETURN) return returnStatement(parser);
        if (token.getToken() == KeywordToken.FUNCTION) return functionDeclaration(parser);
        if (token.getToken() == KeywordToken.WHILE) return whileLoop(parser);
        if (token.getToken() == KeywordToken.FOR) return forLoop(parser);
        if (token.getToken() == KeywordToken.FOREACH) return forEachLoop(parser);
        if (token.getToken() == KeywordToken.BREAK) return breakStatement(parser);
        if (token.getToken() == KeywordToken.CONTINUE) return continueStatement(parser);
        if (token.getToken() == KeywordToken.IMPORT) return importStatement(parser);
        return expressionStatement(parser);
    }

}
