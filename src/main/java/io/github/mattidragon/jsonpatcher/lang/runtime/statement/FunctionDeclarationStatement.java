package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.function.PatchFunction;

import java.util.List;
import java.util.Optional;

public record FunctionDeclarationStatement(String name, Statement body, List<Optional<String>> args, SourceSpan pos) implements Statement {
    public FunctionDeclarationStatement {
        args = List.copyOf(args);
    }

    @Override
    public void run(Context context) {
        context.variables().createVariable(name, new Value.FunctionValue(new PatchFunction.DefinedPatchFunction(body, args, context)), false, pos);
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
