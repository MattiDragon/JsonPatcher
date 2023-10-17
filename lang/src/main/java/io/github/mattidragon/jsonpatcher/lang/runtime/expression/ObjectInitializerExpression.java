package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

import java.util.Map;

public record ObjectInitializerExpression(Map<String, Expression> contents, SourceSpan pos) implements Expression {
    public ObjectInitializerExpression {
        contents = Map.copyOf(contents);
    }

    @Override
    public Value evaluate(EvaluationContext context) {
        var object = new Value.ObjectValue();
        contents.forEach((key, value) -> object.value().put(key, value.evaluate(context)));
        return object;
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
