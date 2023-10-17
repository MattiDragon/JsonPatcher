package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.Parser;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

public record ErrorExpression(Parser.ParseException error) implements Reference {
    @Override
    public Value get(EvaluationContext context) {
        throw new IllegalStateException("Tried to use error expression", error);
    }

    @Override
    public void set(EvaluationContext context, Value value) {
        throw new IllegalStateException("Tried to use error expression", error);
    }

    @Override
    public void delete(EvaluationContext context) {
        throw new IllegalStateException("Tried to use error expression", error);
    }

    @Override
    public SourceSpan getPos() {
        return error.pos;
    }
}
