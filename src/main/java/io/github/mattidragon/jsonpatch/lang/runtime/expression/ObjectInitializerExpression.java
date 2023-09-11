package io.github.mattidragon.jsonpatch.lang.runtime.expression;

import com.google.gson.JsonObject;
import io.github.mattidragon.jsonpatch.lang.runtime.Context;
import io.github.mattidragon.jsonpatch.lang.runtime.Value;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;

import java.util.Map;

public record ObjectInitializerExpression(Map<String, Expression> contents, SourceSpan pos) implements Expression {
    public ObjectInitializerExpression {
        contents = Map.copyOf(contents);
    }

    @Override
    public Value evaluate(Context context) {
        var object = new JsonObject();
        contents.forEach((key, value) -> object.add(key, value.evaluate(context).toGson()));
        return new Value.ObjectValue(object);
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
