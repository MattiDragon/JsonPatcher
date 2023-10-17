package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

public interface Expression {
    Value evaluate(EvaluationContext context);
    SourceSpan getPos();

    default EvaluationException error(String message) {
        return new EvaluationException(message, getPos());
    }
}
