package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;

public interface Statement {
    void run(EvaluationContext context);
    SourceSpan getPos();

    default EvaluationException error(String message) {
        return new EvaluationException(message, getPos());
    }

}
