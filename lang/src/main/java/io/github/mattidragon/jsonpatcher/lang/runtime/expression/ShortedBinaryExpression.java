package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

public record ShortedBinaryExpression(Expression first, Expression second, Operator op, SourceSpan opPos) implements Expression {
    @Override
    public Value evaluate(EvaluationContext context) {
        return op.apply(first, second, context);
    }

    @Override
    public SourceSpan getPos() {
        return opPos;
    }

    public interface Operator {
        Value apply(Expression first, Expression second, EvaluationContext context);
        Operator AND = (first, second, context) -> {
            var firstVal = first.evaluate(context);
            if (!firstVal.asBoolean()) return firstVal;
            return second.evaluate(context);
        };
        Operator OR = (first, second, context) -> {
            var firstVal = first.evaluate(context);
            if (firstVal.asBoolean()) return firstVal;
            return second.evaluate(context);
        };
    }
}
