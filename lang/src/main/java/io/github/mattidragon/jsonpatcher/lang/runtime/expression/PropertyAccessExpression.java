package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

public record PropertyAccessExpression(Expression parent, String name, SourceSpan pos) implements Reference {
    @Override
    public Value get(Context context) {
        var parent = this.parent.evaluate(context);
        if (parent instanceof Value.ObjectValue objectValue) {
            return objectValue.get(name, pos);
        } else {
            throw error("Tried to read property %s of %s. Only objects have properties.".formatted(name, parent));
        }
    }

    @Override
    public void set(Context context, Value value) {
        var parent = this.parent.evaluate(context);
        if (parent instanceof Value.ObjectValue objectValue) {
            objectValue.set(name, value, pos);
        } else {
            throw error("Tried to write property %s of %s. Only objects have properties.".formatted(name, parent));
        }
    }

    @Override
    public void delete(Context context) {
        var parent = this.parent.evaluate(context);
        if (parent instanceof Value.ObjectValue objectValue) {
            objectValue.remove(name, pos);
        } else {
            throw error("Tried to delete property %s of %s. Only objects have properties.".formatted(name, parent));
        }
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
