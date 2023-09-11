package io.github.mattidragon.jsonpatch.lang.runtime;

import io.github.mattidragon.jsonpatch.lang.PositionedException;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import org.jetbrains.annotations.Nullable;

public class EvaluationException extends PositionedException {
    @Nullable
    private final SourceSpan pos;

    public EvaluationException(String message, @Nullable SourceSpan pos) {
        super(message);
        this.pos = pos;
    }

    @Override
    protected String getBaseMessage() {
        return "Error while applying patch";
    }

    @Override
    protected @Nullable SourceSpan getPos() {
        return pos;
    }
}
