package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

public record AssignmentExpression(Reference target, Expression value, BinaryExpression.Operator operator, SourceSpan pos) implements Expression {
    @Override
    public Value evaluate(EvaluationContext context) {
        var original = operator == BinaryExpression.Operator.ASSIGN ? null : target.get(context);
        var value = this.value.evaluate(context);
        target.set(context, operator.apply(original, value, pos));
        return value;
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
