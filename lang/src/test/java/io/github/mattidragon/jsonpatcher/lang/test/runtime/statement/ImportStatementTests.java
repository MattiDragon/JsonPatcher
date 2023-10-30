package io.github.mattidragon.jsonpatcher.lang.test.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.statement.ImportStatement;
import io.github.mattidragon.jsonpatcher.lang.test.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ImportStatementTests {
    @Test
    public void testSimpleImport() {
        var context = EvaluationContext.builder()
                .debugConsumer(TestUtils.EMPTY_DEBUG_CONSUMER)
                .libraryLocator((libraryName, libraryObject, importPos) -> {
                    // test library locator always returns a library
                    libraryObject.value().put("a", new Value.NumberValue(1));
                })
                .build();

        new ImportStatement("lib_name", "test", TestUtils.POS).run(context);
        Assertions.assertTrue(context.variables().hasVariable("test"), "Import should have created the variable");

        context.variables().createVariable("test2", new Value.NumberValue(2), true, TestUtils.POS);
        Assertions.assertThrowsExactly(EvaluationException.class, () -> new ImportStatement("lib_name", "test2", TestUtils.POS).run(context), "Import fail to overwrite existing variables");
    }
}
