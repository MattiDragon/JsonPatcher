package io.github.mattidragon.jsonpatch.lang.runtime.expression;

import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatch.lang.runtime.Context;
import io.github.mattidragon.jsonpatch.lang.runtime.Value;

public record AssignmentExpression(Reference target, Expression value, BinaryExpression.Operator operator, SourceSpan pos) implements Expression {
    @Override
    public Value evaluate(Context context) {
        var original = target.get(context);
        var value = this.value.evaluate(context);
        target.set(context, operator.apply(original, value, pos));
        return value;
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
