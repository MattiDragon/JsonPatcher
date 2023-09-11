package io.github.mattidragon.jsonpatch.lang.runtime.expression;

import io.github.mattidragon.jsonpatch.lang.runtime.Context;
import io.github.mattidragon.jsonpatch.lang.runtime.Value;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;

public record IndexExpression(Expression parent, Expression index, SourceSpan pos) implements Reference {
    @Override
    public Value get(Context context) {
        var parent = this.parent.evaluate(context);
        var index = this.index.evaluate(context);
        if (parent instanceof Value.ObjectValue objectValue) {
            if (!(index instanceof Value.StringValue stringValue))
                throw error("Tried to index object by %s. Objects can only be indexed by string".formatted(index));
            return objectValue.get(stringValue.value(), pos);
        }
        if (parent instanceof Value.ArrayValue arrayValue) {
            if (!(index instanceof Value.NumberValue numberValue))
                throw error("Tried to index array by %s. Arrays can only be indexed by number.".formatted(index));
            return arrayValue.get((int) numberValue.value(), pos);
        }
        throw error("Tried to index %s with %s. Only arrays and objects are indexable.".formatted(parent, index));
    }

    @Override
    public void set(Context context, Value value) {
        var parent = this.parent.evaluate(context);
        var index = this.index.evaluate(context);
        if (parent instanceof Value.ObjectValue objectValue) {
            if (!(index instanceof Value.StringValue stringValue))
                throw error("Tried to index object by %s. Objects can only be indexed by string".formatted(index));
            objectValue.set(stringValue.value(), value, pos);
        }
        if (parent instanceof Value.ArrayValue arrayValue) {
            if (!(index instanceof Value.NumberValue numberValue))
                throw error("Tried to index array by %s. Arrays can only be indexed by number.".formatted(index));
            arrayValue.set((int) numberValue.value(), value, pos);
        }
        throw error("Tried to index %s with %s. Only arrays and objects are indexable.".formatted(parent, index));
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
