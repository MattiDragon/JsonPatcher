package io.github.mattidragon.jsonpatch.lang.runtime.expression;

import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatch.lang.runtime.Context;
import io.github.mattidragon.jsonpatch.lang.runtime.Value;

import java.util.List;

public record FunctionCallExpression(String name, List<Expression> arguments, SourceSpan pos) implements Expression {
    @Override
    public Value evaluate(Context context) {
        return context.variables()
                .getFunction(name, pos)
                .execute(context,
                        arguments.stream().map(expression -> expression.evaluate(context)).toList(),
                        pos);
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
