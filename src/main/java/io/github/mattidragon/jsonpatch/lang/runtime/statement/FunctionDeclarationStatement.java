package io.github.mattidragon.jsonpatch.lang.runtime.statement;

import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatch.lang.runtime.Context;
import io.github.mattidragon.jsonpatch.lang.runtime.function.PatchFunction;

import java.util.List;

public record FunctionDeclarationStatement(String name, BlockStatement body, List<String> args, SourceSpan pos) implements Statement {
    public FunctionDeclarationStatement {
        args = List.copyOf(args);
    }

    @Override
    public void run(Context context) {
        context.variables().defineFunction(name, new PatchFunction.DefinedPatchFunction(body, args), pos);
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
