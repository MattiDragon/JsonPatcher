package io.github.mattidragon.jsonpatcher.lang.test;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceFile;
import io.github.mattidragon.jsonpatcher.lang.parse.SourcePos;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Expression;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.ValueExpression;
import io.github.mattidragon.jsonpatcher.lang.runtime.statement.BlockStatement;
import io.github.mattidragon.jsonpatcher.lang.runtime.statement.Statement;
import io.github.mattidragon.jsonpatcher.lang.runtime.statement.UnnecessarySemicolonStatement;

import java.util.List;
import java.util.function.Consumer;

public class TestUtils {
    public static final SourceFile FILE = new SourceFile("test file", "00");
    public static final SourceSpan POS = new SourceSpan(new SourcePos(FILE, 1, 1), new SourcePos(FILE, 1, 2));
    public static final Consumer<Value> EMPTY_DEBUG_CONSUMER = (value) -> {};

    public static EvaluationContext createTestContext() {
        return EvaluationContext.builder().debugConsumer(EMPTY_DEBUG_CONSUMER).build();
    }

    public static Expression trueExpression() {
        return new ValueExpression(Value.BooleanValue.of(true), POS);
    }

    public static Expression falseExpression() {
        return new ValueExpression(Value.BooleanValue.of(false), POS);
    }

    public static Expression nullExpression() {
        return new ValueExpression(Value.NullValue.NULL, POS);
    }

    public static Expression stringExpression(String value) {
        return new ValueExpression(new Value.StringValue(value), POS);
    }

    public static Expression numberExpression(double value) {
        return new ValueExpression(new Value.NumberValue(value), POS);
    }

    public static Statement emptyStatement() {
        return new UnnecessarySemicolonStatement(POS);
    }

    public static Statement blockStatement(Statement... statements) {
        return new BlockStatement(List.of(statements), POS);
    }
}
