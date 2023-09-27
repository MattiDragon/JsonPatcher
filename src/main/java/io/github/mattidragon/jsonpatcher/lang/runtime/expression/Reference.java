package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

/**
 * An expression which can also be assigned to.
 */
public interface Reference extends Expression {
    Value get(Context context);

    void set(Context context, Value value);

    @Override
    default Value evaluate(Context context) {
        return get(context);
    }

    void delete(Context context);
}
