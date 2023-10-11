package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.function.PatchFunction;
import io.github.mattidragon.jsonpatcher.lang.runtime.statement.Statement;

import java.util.List;
import java.util.Optional;

public record FunctionExpression(Statement body, List<Optional<String>> args, SourceSpan pos) implements Expression {
    public FunctionExpression {
        args = List.copyOf(args);
    }

    @Override
    public Value evaluate(Context context) {
        return new Value.FunctionValue(new PatchFunction.DefinedPatchFunction(body, args, context));
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
