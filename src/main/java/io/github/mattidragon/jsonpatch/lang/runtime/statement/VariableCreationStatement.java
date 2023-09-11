package io.github.mattidragon.jsonpatch.lang.runtime.statement;

import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatch.lang.runtime.Context;
import io.github.mattidragon.jsonpatch.lang.runtime.expression.Expression;

public record VariableCreationStatement(String name, Expression initializer, boolean mutable, SourceSpan pos) implements Statement {
    @Override
    public void run(Context context) {
        context.variables().create(name, initializer.evaluate(context), mutable, pos);
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
