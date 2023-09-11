package io.github.mattidragon.jsonpatch.lang.runtime.expression;

import io.github.mattidragon.jsonpatch.lang.runtime.Context;
import io.github.mattidragon.jsonpatch.lang.runtime.Value;

/**
 * An expression which can also be assigned to.
 */
public interface Reference extends Expression {
    Value get(Context context);

    void set(Context context, Value v);

    @Override
    default Value evaluate(Context context) {
        return get(context);
    }
}
