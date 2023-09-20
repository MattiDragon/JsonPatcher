package io.github.mattidragon.jsonpatch.lang.runtime.statement;

import io.github.mattidragon.jsonpatch.lang.parse.Parser;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatch.lang.runtime.Context;

public record ErrorStatement(Parser.ParseException error) implements Statement {
    @Override
    public void run(Context context) {
        throw new IllegalStateException("Tried to execute error statement", error);
    }

    @Override
    public SourceSpan getPos() {
        return error.pos;
    }
}
