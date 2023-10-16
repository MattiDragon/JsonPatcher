package io.github.mattidragon.jsonpatcher.lang.runtime.function;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

/**
 * This exception is used for return statements.
 * Using a java exception makes this easier than handling returns at every point in the interpreter;
 */
public class ReturnException extends RuntimeException {
    public final Value value;
    public final SourceSpan pos;

    public ReturnException(Value value, SourceSpan pos) {
        super("A return exception wasn't handled. Something is wrong.");
        this.value = value;
        this.pos = pos;
    }
}
