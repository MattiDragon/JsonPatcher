package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.Parser;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;

public record ErrorStatement(Parser.ParseException error) implements Statement {
    @Override
    public void run(EvaluationContext context) {
        throw new IllegalStateException("Tried to execute error statement", error);
    }

    @Override
    public SourceSpan getPos() {
        return error.pos;
    }
}
