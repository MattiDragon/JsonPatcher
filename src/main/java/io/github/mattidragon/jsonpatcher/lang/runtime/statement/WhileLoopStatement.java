package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Expression;

public record WhileLoopStatement(Expression condition, Statement body, SourceSpan pos) implements Statement {
    @Override
    public void run(Context context) {
        while (condition.evaluate(context).asBoolean()) {
            body.run(context);
        }
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
