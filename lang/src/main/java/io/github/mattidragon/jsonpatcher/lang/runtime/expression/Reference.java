package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

/**
 * An expression which can also be assigned to.
 */
public interface Reference extends Expression {
    Value get(EvaluationContext context);

    void set(EvaluationContext context, Value value);

    void delete(EvaluationContext context);

    @Override
    default Value evaluate(EvaluationContext context) {
        return get(context);
    }
}
