package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Expression;
import org.jetbrains.annotations.Nullable;

public record IfStatement(Expression condition, Statement action, @Nullable Statement elseAction, SourceSpan pos) implements Statement {
    @Override
    public void run(EvaluationContext context) {
        if (condition.evaluate(context).asBoolean()) {
            action.run(context);
        } else if (elseAction != null) {
            elseAction.run(context);
        }
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
