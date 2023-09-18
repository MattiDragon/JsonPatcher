package io.github.mattidragon.jsonpatch.lang.runtime.expression;

import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatch.lang.runtime.Context;
import io.github.mattidragon.jsonpatch.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatch.lang.runtime.Value;

public record UnaryExpression(Expression input, Operator op, SourceSpan pos) implements Expression {
    @Override
    public Value evaluate(Context context) {
        return op.apply(input.evaluate(context), pos);
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }

    public interface Operator {
        Value apply(Value value, SourceSpan pos);

        Operator NOT = (value, pos) -> {
            if (value instanceof Value.BooleanValue booleanValue) return Value.BooleanValue.of(!booleanValue.value());
            throw new EvaluationException("Can't apply boolean not to %s. Only booleans are supported.".formatted(value), pos);
        };
        Operator MINUS = (value, pos) -> {
            if (value instanceof Value.NumberValue numberValue) return new Value.NumberValue(-numberValue.value());
            throw new EvaluationException("Can't negate %s. Only numbers are supported.".formatted(value), pos);
        };
        Operator BITWISE_NOT = (value, pos) -> {
            if (value instanceof Value.NumberValue numberValue) return new Value.NumberValue(~(int) numberValue.value());
            throw new EvaluationException("Can't apply bitwise not to %s. Only numbers are supported.".formatted(value), pos);
        };
    }
}
