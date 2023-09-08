package io.github.mattidragon.jsonpatch.lang.ast.expression;

import io.github.mattidragon.jsonpatch.lang.ast.Context;
import io.github.mattidragon.jsonpatch.lang.ast.Value;

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
