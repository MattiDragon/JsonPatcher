package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Expression;

public record ForLoopStatement(Statement initializer, Expression condition, Statement incrementer, Statement body, SourceSpan pos) implements Statement {
    @Override
    public void run(EvaluationContext context) {
        context = context.newScope();
        for (initializer.run(context); condition.evaluate(context).asBoolean(); incrementer.run(context)) {
            try {
                body.run(context);
            } catch (BreakStatement.BreakException e) {
                break;
            } catch (ContinueStatement.ContinueException e) {
                // Continue
            }
        }
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
