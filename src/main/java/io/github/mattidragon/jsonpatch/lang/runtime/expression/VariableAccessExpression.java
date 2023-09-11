package io.github.mattidragon.jsonpatch.lang.runtime.expression;

import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatch.lang.runtime.Context;
import io.github.mattidragon.jsonpatch.lang.runtime.Value;

public record VariableAccessExpression(String name, SourceSpan pos) implements Reference {
    @Override
    public SourceSpan getPos() {
        return pos;
    }

    @Override
    public Value get(Context context) {
        return context.variables().get(name, pos);
    }

    @Override
    public void set(Context context, Value value) {
        context.variables().set(name, value, pos);
    }
}
