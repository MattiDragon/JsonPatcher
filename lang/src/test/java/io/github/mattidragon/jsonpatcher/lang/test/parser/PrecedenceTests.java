package io.github.mattidragon.jsonpatcher.lang.test.parser;

import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.test.TestUtils;
import org.junit.jupiter.api.Test;

public class PrecedenceTests {
    @Test
    public void testSumProductPrecedence() {
        TestUtils.testExpression("1 + 2 * 3", new Value.NumberValue(7));
        TestUtils.testExpression("1 * 2 + 3", new Value.NumberValue(5));

        TestUtils.testExpression("1 + 2 * 3 + 4", new Value.NumberValue(11));
        TestUtils.testExpression("1 * 2 + 3 * 4", new Value.NumberValue(14));
    }

    @Test
    public void testLogicEqualityPrecedence() {
        TestUtils.testExpression("true == false && true", Value.BooleanValue.FALSE);
        TestUtils.testExpression("true && true != false", Value.BooleanValue.TRUE);

        TestUtils.testExpression("true == false || true", Value.BooleanValue.TRUE);
        TestUtils.testExpression("true || true != false", Value.BooleanValue.TRUE);
    }

    @Test
    public void testPrefixPrecedence() {
        TestUtils.testExpression("!true == false", Value.BooleanValue.TRUE);
        TestUtils.testExpression("!true != false", Value.BooleanValue.FALSE);

        TestUtils.testExpression("-1 + 2", new Value.NumberValue(1));
        TestUtils.testExpression("-1 - 2", new Value.NumberValue(-3));
    }
}
