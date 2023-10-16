package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

public record ImportExpression(String libraryName, SourceSpan pos) implements Expression {
    @Override
    public Value evaluate(Context context) {
        return context.findLibrary(libraryName, pos);
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
