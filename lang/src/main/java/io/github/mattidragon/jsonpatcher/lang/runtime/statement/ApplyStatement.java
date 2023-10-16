package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Expression;

public record ApplyStatement(Expression root, Statement action, SourceSpan pos) implements Statement {
    @Override
    public void run(Context context) {
        var root = this.root.evaluate(context);
        if (!(root instanceof Value.ObjectValue objectValue)) {
            throw error("Only objects can be used in apply statements, tried to use %s".formatted(root));
        }
        action.run(context.withRoot(objectValue));
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
