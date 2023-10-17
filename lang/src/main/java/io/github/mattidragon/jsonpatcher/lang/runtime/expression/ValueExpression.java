package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

public record ValueExpression(Value.Primitive value, SourceSpan pos) implements Expression {
    @Override
    public Value evaluate(EvaluationContext context) {
        return value;
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
