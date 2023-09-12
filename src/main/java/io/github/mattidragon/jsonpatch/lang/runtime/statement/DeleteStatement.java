package io.github.mattidragon.jsonpatch.lang.runtime.statement;

import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatch.lang.runtime.Context;
import io.github.mattidragon.jsonpatch.lang.runtime.expression.Reference;

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
