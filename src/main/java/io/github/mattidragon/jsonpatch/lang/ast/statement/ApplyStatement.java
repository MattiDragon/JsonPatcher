package io.github.mattidragon.jsonpatch.lang.ast.statement;

import io.github.mattidragon.jsonpatch.lang.ast.Context;
import io.github.mattidragon.jsonpatch.lang.ast.Value;
import io.github.mattidragon.jsonpatch.lang.ast.expression.Expression;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;

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
