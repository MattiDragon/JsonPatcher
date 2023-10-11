package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;

public record UnnecessarySemicolonStatement(SourceSpan pos) implements Statement {
    @Override
    public void run(Context context) {
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
