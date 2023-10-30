package io.github.mattidragon.jsonpatcher.lang.test.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.runtime.statement.BlockStatement;
import io.github.mattidragon.jsonpatcher.lang.runtime.statement.BreakStatement;
import io.github.mattidragon.jsonpatcher.lang.runtime.statement.WhileLoopStatement;
import io.github.mattidragon.jsonpatcher.lang.test.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class LoopStatementTests {
    @Test
    public void testWhileBreak() {
        var context = TestUtils.createTestContext();
        var statement = new WhileLoopStatement(TestUtils.trueExpression(), new BlockStatement(List.of(
                new BreakStatement(TestUtils.POS)
        ), TestUtils.POS), TestUtils.POS);

        Assertions.assertTimeoutPreemptively(Duration.of(10, ChronoUnit.MILLIS), () -> statement.run(context), "While loop with break took too long to execute");
    }
}
