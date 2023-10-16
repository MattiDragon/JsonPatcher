package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

public record UnaryModificationExpression(boolean postfix, Reference target, UnaryExpression.Operator operator, SourceSpan pos) implements Expression {
    @Override
    public Value evaluate(Context context) {
        var oldValue = target.get(context);
        var newValue = operator.apply(oldValue, pos);
        target.set(context, newValue);

        return postfix ? oldValue : newValue;
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
