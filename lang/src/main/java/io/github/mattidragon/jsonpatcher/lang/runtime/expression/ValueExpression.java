package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

public record ValueExpression(Value.Primitive value, SourceSpan pos) implements Expression {
    @Override
    public Value evaluate(Context context) {
        return value;
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
