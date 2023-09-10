package io.github.mattidragon.jsonpatch.lang.ast.statement;

import io.github.mattidragon.jsonpatch.lang.ast.Context;
import io.github.mattidragon.jsonpatch.lang.ast.EvaluationException;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;

public interface Statement {
    void run(Context context);
    SourceSpan getPos();

    default EvaluationException error(String message) {
        return new EvaluationException(message, getPos());
    }

}
