package io.github.mattidragon.jsonpatcher.lang.test;

import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value.NumberValue;
import io.github.mattidragon.jsonpatcher.lang.runtime.VariableStack;
import org.junit.jupiter.api.Test;

import static io.github.mattidragon.jsonpatcher.lang.test.TestUtils.POS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;


public class VariableStackTests {
    @Test
    public void testVariableCreationRules() {
        var variables = new VariableStack();
        assertDoesNotThrow(() -> variables.createVariable("a", new NumberValue(1), false, POS), "Creating a variable should not throw");
        assertThrowsExactly(EvaluationException.class, () -> variables.createVariable("a", new NumberValue(1), false, POS), "Creating a variable with the same name should throw");
        assertThrowsExactly(EvaluationException.class, () -> variables.createVariable("a", new NumberValue(2), true, POS), "Creating a variable with the same name and different mutability should throw");

        var block = new VariableStack(variables);
        assertThrowsExactly(EvaluationException.class, () -> block.createVariable("a", new NumberValue(1), false, POS), "Creating a variable with the same name in a child block should throw");
        assertThrowsExactly(EvaluationException.class, () -> block.createVariable("a", new NumberValue(2), true, POS), "Creating a variable with the same name and different mutability in a child block should throw");
        assertDoesNotThrow(() -> block.createVariable("b", new NumberValue(1), false, POS), "Creating a variable with a different name in a child block should not throw");

        assertDoesNotThrow(() -> variables.createVariable("b", new NumberValue(1), false, POS), "Creating a variable with a same name in a parent block after child should not throw");
    }
}
