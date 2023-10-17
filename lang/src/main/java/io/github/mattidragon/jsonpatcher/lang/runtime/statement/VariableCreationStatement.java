package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Expression;

public record VariableCreationStatement(String name, Expression initializer, boolean mutable, SourceSpan pos) implements Statement {
    @Override
    public void run(EvaluationContext context) {
        context.variables().createVariable(name, initializer.evaluate(context), mutable, pos);
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
