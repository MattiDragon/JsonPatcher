package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.Parser;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

public record ErrorExpression(Parser.ParseException error) implements Reference {
    @Override
    public Value get(Context context) {
        throw new IllegalStateException("Tried to use error expression", error);
    }

    @Override
    public void set(Context context, Value value) {
        throw new IllegalStateException("Tried to use error expression", error);
    }

    @Override
    public void delete(Context context) {
        throw new IllegalStateException("Tried to use error expression", error);
    }

    @Override
    public SourceSpan getPos() {
        return error.pos;
    }
}
