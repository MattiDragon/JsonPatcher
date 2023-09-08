package io.github.mattidragon.jsonpatch.lang.ast.expression;

import io.github.mattidragon.jsonpatch.lang.ast.Context;
import io.github.mattidragon.jsonpatch.lang.ast.EvaluationException;
import io.github.mattidragon.jsonpatch.lang.ast.Value;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;

public interface Expression {
    Value evaluate(Context context);
    SourceSpan getPos();

    default EvaluationException error(String message) {
        return new EvaluationException(message, getPos());
    }
}
