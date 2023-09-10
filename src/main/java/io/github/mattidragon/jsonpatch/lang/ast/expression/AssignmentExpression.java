package io.github.mattidragon.jsonpatch.lang.ast.expression;

import io.github.mattidragon.jsonpatch.lang.ast.Context;
import io.github.mattidragon.jsonpatch.lang.ast.Value;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;

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
