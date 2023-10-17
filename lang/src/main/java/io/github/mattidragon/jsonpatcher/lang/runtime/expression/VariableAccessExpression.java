package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

public record VariableAccessExpression(String name, SourceSpan pos) implements Reference {
    @Override
    public SourceSpan getPos() {
        return pos;
    }

    @Override
    public Value get(EvaluationContext context) {
        return context.variables().getVariable(name, pos);
    }

    @Override
    public void set(EvaluationContext context, Value value) {
        context.variables().setVariable(name, value, pos);
    }

    @Override
    public void delete(EvaluationContext context) {
        context.variables().deleteVariable(name, pos);
    }
}
