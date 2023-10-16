package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Expression;

public record WhileLoopStatement(Expression condition, Statement body, SourceSpan pos) implements Statement {
    @Override
    public void run(Context context) {
        while (condition.evaluate(context).asBoolean()) {
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
