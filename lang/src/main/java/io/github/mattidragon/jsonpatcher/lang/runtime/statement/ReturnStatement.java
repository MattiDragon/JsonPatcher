package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Expression;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.ValueExpression;
import io.github.mattidragon.jsonpatcher.lang.runtime.function.ReturnException;

import java.util.Optional;

public record ReturnStatement(Optional<Expression> value, SourceSpan pos) implements Statement {
    @Override
    public void run(Context context) {
        throw new ReturnException(value.orElseGet(() -> new ValueExpression(Value.NullValue.NULL, pos)).evaluate(context), pos);
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
