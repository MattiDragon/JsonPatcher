package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

import java.util.List;

public record ArrayInitializerExpression(List<Expression> contents, SourceSpan pos) implements Expression {
    public ArrayInitializerExpression {
        contents = List.copyOf(contents);
    }

    @Override
    public Value evaluate(EvaluationContext context) {
        return new Value.ArrayValue(contents.stream()
                .map(expression -> expression.evaluate(context))
                .toList());
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
