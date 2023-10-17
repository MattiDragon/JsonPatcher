package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

public record IsInstanceExpression(Expression input, Type type, SourceSpan pos) implements Expression {
    @Override
    public Value evaluate(EvaluationContext context) {
        var value = input.evaluate(context);
        var matches = switch (type) {
            case NUMBER -> value instanceof Value.NumberValue;
            case STRING -> value instanceof Value.StringValue;
            case BOOLEAN -> value instanceof Value.BooleanValue;
            case ARRAY -> value instanceof Value.ArrayValue;
            case OBJECT -> value instanceof Value.ObjectValue;
            case NULL -> value instanceof Value.NullValue;
            case FUNCTION -> value instanceof Value.FunctionValue;
        };
        return Value.BooleanValue.of(matches);
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }

    public enum Type {
        NUMBER,
        STRING,
        BOOLEAN,
        ARRAY,
        OBJECT,
        NULL,
        FUNCTION
    }
}
