package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

import java.util.List;

public record FunctionCallExpression(Expression function, List<Expression> arguments, SourceSpan pos) implements Expression {
    @Override
    public Value evaluate(EvaluationContext context) {
        var function = this.function.evaluate(context);
        if (!(function instanceof Value.FunctionValue functionValue)) {
            throw new EvaluationException("Tried to call %s, not a function".formatted(function), pos);
        }

        return functionValue.function().execute(
                context,
                arguments.stream().map(expression -> expression.evaluate(context)).toList(),
                pos
        );
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
