package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;

public record BreakStatement(SourceSpan pos) implements Statement {
    @Override
    public void run(Context context) {
        throw new BreakException();
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }

    public static class BreakException extends RuntimeException {
        public BreakException() {
            super("Uncaught break statement");
        }
    }
}
