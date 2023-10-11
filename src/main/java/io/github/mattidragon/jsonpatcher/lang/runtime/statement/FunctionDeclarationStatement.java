package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.FunctionExpression;

public record FunctionDeclarationStatement(String name, FunctionExpression value) implements Statement {
    @Override
    public void run(Context context) {
        context.variables().createVariable(name, value.evaluate(context), false, value.pos());
    }

    @Override
    public SourceSpan getPos() {
        return value.getPos();
    }
}
