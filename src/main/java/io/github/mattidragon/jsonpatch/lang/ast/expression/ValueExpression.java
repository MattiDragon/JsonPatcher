package io.github.mattidragon.jsonpatch.lang.ast.expression;

import io.github.mattidragon.jsonpatch.lang.ast.Context;
import io.github.mattidragon.jsonpatch.lang.ast.Value;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;

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
