package io.github.mattidragon.jsonpatcher.lang.test.runtime.libraries;

import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.stdlib.Libraries;
import io.github.mattidragon.jsonpatcher.lang.test.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class DebugLibraryTests {
    private static final Libraries.DebugLibrary LIBRARY = new Libraries.DebugLibrary();

    @Test
    public void testThrow() {
        var context = TestUtils.createTestFunctionContext();
        assertThrowsExactly(EvaluationException.class, () -> LIBRARY.throw_(context, new Value.StringValue("Test error")), "Throwing an exception should throw");
    }

    @Test
    public void testAssert() {
        var context = TestUtils.createTestFunctionContext();
        assertThrowsExactly(EvaluationException.class, () -> LIBRARY.assert_(context, Value.BooleanValue.FALSE), "Asserting false should throw");
        assertThrowsExactly(EvaluationException.class, () -> LIBRARY.assert_(context, Value.NullValue.NULL), "Asserting null should throw");
    }
}
