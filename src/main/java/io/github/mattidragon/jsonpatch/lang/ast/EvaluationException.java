package io.github.mattidragon.jsonpatch.lang.ast;

import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import org.jetbrains.annotations.Nullable;

public class EvaluationException extends RuntimeException {
    @Nullable
    public final SourceSpan pos;

    public EvaluationException(String message, @Nullable SourceSpan pos) {
        super(message);
        this.pos = pos;
    }
}
