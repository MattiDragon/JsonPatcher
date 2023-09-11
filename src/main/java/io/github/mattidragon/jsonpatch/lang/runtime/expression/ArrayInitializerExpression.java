package io.github.mattidragon.jsonpatch.lang.runtime.expression;

import com.google.gson.JsonArray;
import io.github.mattidragon.jsonpatch.lang.runtime.Context;
import io.github.mattidragon.jsonpatch.lang.runtime.Value;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;

import java.util.List;

public record ArrayInitializerExpression(List<Expression> contents, SourceSpan pos) implements Expression {
    public ArrayInitializerExpression {
        contents = List.copyOf(contents);
    }

    @Override
    public Value evaluate(Context context) {
        var array = new JsonArray(contents.size());
        contents.stream()
                .map(expression -> expression.evaluate(context))
                .map(Value::toGson)
                .forEach(array::add);
        return new Value.ArrayValue(array);
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
