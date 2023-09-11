package io.github.mattidragon.jsonpatch.lang.runtime.expression;

import io.github.mattidragon.jsonpatch.lang.runtime.Context;
import io.github.mattidragon.jsonpatch.lang.runtime.Value;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;

public record PropertyAccessExpression(Expression parent, String name, SourceSpan pos) implements Reference {
    @Override
    public Value get(Context context) {
        var parent = this.parent.evaluate(context);
        if (parent instanceof Value.ObjectValue objectValue) return objectValue.get(name, pos);
        throw error("Tried to read property %s of %s. Only objects have properties.".formatted(name, parent));
    }

    @Override
    public void set(Context context, Value v) {
        var parent = this.parent.evaluate(context);
        if (parent instanceof Value.ObjectValue objectValue) {
            objectValue.set(name, v, pos);
        } else {
            throw error("Tried to read property %s of %s. Only objects have properties.".formatted(name, parent));
        }
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
