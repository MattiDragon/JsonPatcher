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
import io.github.mattidragon.jsonpatcher.lang.runtime.stdlib.LibraryBuilder;
import org.junit.jupiter.api.AssertionFailureBuilder;

import java.util.List;
import java.util.function.Consumer;

public class TestUtils {
    public static final SourceFile FILE = new SourceFile("test file", "00");
    public static final SourceSpan POS = new SourceSpan(new SourcePos(FILE, 1, 1), new SourcePos(FILE, 1, 2));
    public static final Consumer<Value> EMPTY_DEBUG_CONSUMER = (value) -> {};

    public static EvaluationContext createTestContext() {
        return EvaluationContext.builder().debugConsumer(EMPTY_DEBUG_CONSUMER).build();
    }

    public static LibraryBuilder.FunctionContext createTestFunctionContext() {
        return new LibraryBuilder.FunctionContext(createTestContext(), POS);
    }

    public static void assertEquals(Value expected, Value actual) {
        if (!areEqual(expected, actual)) {
            AssertionFailureBuilder.assertionFailure().expected(expected).actual(actual).buildAndThrow();
        }
    }

    public static boolean areEqual(Value v1, Value v2) {
        if (v1.equals(v2)) return true;

        if (v1 instanceof Value.ObjectValue o1 && v2 instanceof Value.ObjectValue o2) {
            if (o1.value().size() != o2.value().size()) return false;

            for (var entry : o1.value().entrySet()) {
                if (!o2.value().containsKey(entry.getKey())) return false;
                if (!areEqual(entry.getValue(), o2.value().get(entry.getKey()))) return false;
            }
            return true;
        }

        if (v1 instanceof Value.ArrayValue a1 && v2 instanceof Value.ArrayValue a2) {
            if (a1.value().size() != a2.value().size()) return false;

            for (int i = 0; i < a1.value().size(); i++) {
                if (!areEqual(a1.value().get(i), a2.value().get(i))) return false;
            }
            return true;
        }

        return false;
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
