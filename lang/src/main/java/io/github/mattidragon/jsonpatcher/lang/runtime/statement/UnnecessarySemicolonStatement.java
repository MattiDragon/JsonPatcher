package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;

public record UnnecessarySemicolonStatement(SourceSpan pos) implements Statement {
    @Override
    public void run(EvaluationContext context) {
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
