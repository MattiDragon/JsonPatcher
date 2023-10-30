package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;

public record ImportStatement(String libraryName, String variableName, SourceSpan pos) implements Statement {
    @Override
    public void run(EvaluationContext context) {
        context.variables().createVariable(variableName, context.findLibrary(libraryName, pos), false, pos);
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
