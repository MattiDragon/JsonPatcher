package io.github.mattidragon.jsonpatcher.lang.test.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.AssignmentExpression;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.BinaryExpression;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.ImplicitRootExpression;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.ValueExpression;
import io.github.mattidragon.jsonpatcher.lang.test.TestUtils;
import org.junit.jupiter.api.Test;

import static io.github.mattidragon.jsonpatcher.lang.test.TestUtils.POS;
import static org.junit.jupiter.api.Assertions.*;

public class AssignmentExpressionTests {
    @Test
    public void testSimpleAssignment() {
        var context = TestUtils.createTestContext();

        assertDoesNotThrow(() -> {
            new AssignmentExpression(new ImplicitRootExpression("a", POS), new ValueExpression(new Value.NumberValue(1), POS), BinaryExpression.Operator.ASSIGN, POS)
                    .evaluate(context);
        }, "Assignment should not throw");
        assertTrue(context.root().value().containsKey("a"), "Assignment should have created the property");
        assertEquals(new Value.NumberValue(1), context.root().value().get("a"), "Assignment should have set the property to the correct value");
    }

    @Test
    public void testPlusAssignment() {
        var context = TestUtils.createTestContext();
        context.root().value().put("a", new Value.NumberValue(1));

        assertDoesNotThrow(() -> {
            new AssignmentExpression(new ImplicitRootExpression("a", POS), new ValueExpression(new Value.NumberValue(2), POS), BinaryExpression.Operator.PLUS, POS)
                    .evaluate(context);
        }, "Assignment should not throw");

        assertEquals(new Value.NumberValue(3), context.root().value().get("a"), "Assignment should have set the property to the correct value");
    }
}
