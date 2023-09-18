package io.github.mattidragon.jsonpatch.lang.runtime.expression;

import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatch.lang.runtime.Context;
import io.github.mattidragon.jsonpatch.lang.runtime.Value;

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
