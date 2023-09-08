package io.github.mattidragon.jsonpatch.lang.ast.expression;

import io.github.mattidragon.jsonpatch.lang.ast.Context;
import io.github.mattidragon.jsonpatch.lang.ast.EvaluationException;
import io.github.mattidragon.jsonpatch.lang.ast.Value;
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
    public void set(Context context, Value v) {
        var parent = this.parent.evaluate(context);
        var index = this.index.evaluate(context);
        if (parent instanceof Value.ObjectValue objectValue) {
            if (!(index instanceof Value.StringValue stringValue))
                throw error("Tried to index object by %s. Objects can only be indexed by string".formatted(index));
            objectValue.set(stringValue.value(), v, pos);
        }
        if (parent instanceof Value.ArrayValue arrayValue) {
            if (!(index instanceof Value.NumberValue numberValue))
                throw error("Tried to index array by %s. Arrays can only be indexed by number.".formatted(index));
            arrayValue.set((int) numberValue.value(), v, pos);
        }
        throw error("Tried to index %s with %s. Only arrays and objects are indexable.".formatted(parent, index));
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
