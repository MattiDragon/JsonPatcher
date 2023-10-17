package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

public record RootExpression(SourceSpan pos) implements Expression {
    @Override
    public Value evaluate(EvaluationContext context) {
        return context.root();
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
