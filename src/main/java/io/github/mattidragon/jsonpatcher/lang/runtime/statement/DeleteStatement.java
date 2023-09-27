package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Reference;

public record DeleteStatement(Reference target, SourceSpan pos) implements Statement {
    @Override
    public void run(Context context) {
        target.delete(context);
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
