package io.github.mattidragon.jsonpatcher.lang.test.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value.*;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.BinaryExpression;
import io.github.mattidragon.jsonpatcher.lang.test.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BinaryExpressionTests {
    // TODO: implement more tests. Not very high priority as these are unlikely to be broken
    // There's no real reason to test the actual expression as it's trivial. Instead, we test the operator implementations.
    @Test
    public void testPlus() {
        var op = BinaryExpression.Operator.PLUS;

        assertEquals(new NumberValue(3),
                op.apply(new NumberValue(1),
                        new NumberValue(2),
                        TestUtils.POS));

        assertEquals(new StringValue("12"),
                op.apply(new StringValue("1"),
                        new StringValue("2"),
                        TestUtils.POS));

        TestUtils.assertEquals(
                new ArrayValue(List.of(new NumberValue(1), new NumberValue(2))),
                op.apply(new ArrayValue(List.of(new NumberValue(1))),
                        new ArrayValue(List.of(new NumberValue(2))),
                        TestUtils.POS));

        TestUtils.assertEquals(
                new ObjectValue(Map.of("a", new NumberValue(1), "b", new NumberValue(2))),
                op.apply(new ObjectValue(Map.of("a", new NumberValue(1))),
                        new ObjectValue(Map.of("b", new NumberValue(2))),
                        TestUtils.POS));

        assertThrowsExactly(EvaluationException.class, () -> op.apply(new NumberValue(1), new StringValue("2"), TestUtils.POS));
        assertThrowsExactly(EvaluationException.class, () -> op.apply(new StringValue("1"), new NumberValue(2), TestUtils.POS));
        assertThrowsExactly(EvaluationException.class, () -> op.apply(new ArrayValue(List.of(new NumberValue(1))), new NumberValue(2), TestUtils.POS));
    }
}
