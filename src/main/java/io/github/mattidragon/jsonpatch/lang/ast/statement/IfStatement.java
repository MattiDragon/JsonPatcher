package io.github.mattidragon.jsonpatch.lang.ast.statement;

import io.github.mattidragon.jsonpatch.lang.ast.Context;
import io.github.mattidragon.jsonpatch.lang.ast.expression.Expression;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import org.jetbrains.annotations.Nullable;

public record IfStatement(Expression condition, Statement action, @Nullable Statement elseAction, SourceSpan pos) implements Statement {
    @Override
    public void run(Context context) {
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
