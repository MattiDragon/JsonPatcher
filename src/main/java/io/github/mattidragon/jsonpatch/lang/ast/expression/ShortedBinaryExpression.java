package io.github.mattidragon.jsonpatch.lang.ast.expression;

import io.github.mattidragon.jsonpatch.lang.ast.Context;
import io.github.mattidragon.jsonpatch.lang.ast.Value;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;

public record ShortedBinaryExpression(Expression first, Expression second, Operator op, SourceSpan opPos) implements Expression {
    @Override
    public Value evaluate(Context context) {
        return op.apply(first, second, context);
    }

    @Override
    public SourceSpan getPos() {
        return opPos;
    }

    public interface Operator {
        Value apply(Expression first, Expression second, Context context);
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
