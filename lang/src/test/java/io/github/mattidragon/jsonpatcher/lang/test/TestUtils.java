package io.github.mattidragon.jsonpatcher.lang.test;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceFile;
import io.github.mattidragon.jsonpatcher.lang.parse.SourcePos;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Expression;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.ValueExpression;
import io.github.mattidragon.jsonpatcher.lang.runtime.statement.Statement;
import io.github.mattidragon.jsonpatcher.lang.runtime.statement.UnnecessarySemicolonStatement;

import java.util.function.Consumer;

public class TestUtils {
    public static final SourceFile FILE = new SourceFile("test file", "00");
    public static final SourceSpan POS = new SourceSpan(new SourcePos(FILE, 1, 1), new SourcePos(FILE, 1, 2));
    public static final EvaluationContext.LibraryLocator NO_OP_LIBRARY_LOCATOR = (libraryName, libraryObject, importPos) -> {
        throw new AssertionError("No imports should be loaded");
    };
    public static final Consumer<Value> EMPTY_DEBUG_CONSUMER = (value) -> {};

    public static EvaluationContext createTestContext() {
        return EvaluationContext.create(new Value.ObjectValue(), NO_OP_LIBRARY_LOCATOR, EMPTY_DEBUG_CONSUMER);
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
}
