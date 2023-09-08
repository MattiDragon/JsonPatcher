package io.github.mattidragon.jsonpatch.lang.ast.expression;

import io.github.mattidragon.jsonpatch.lang.ast.Context;
import io.github.mattidragon.jsonpatch.lang.ast.Value;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;

public record RootExpression(SourceSpan pos) implements Expression {
    @Override
    public Value evaluate(Context context) {
        return context.root();
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
