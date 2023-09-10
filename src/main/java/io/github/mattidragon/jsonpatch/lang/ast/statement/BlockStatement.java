package io.github.mattidragon.jsonpatch.lang.ast.statement;

import io.github.mattidragon.jsonpatch.lang.ast.Context;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;

import java.util.List;

public record BlockStatement(List<Statement> statements, SourceSpan pos) implements Statement {
    public BlockStatement {
        statements = List.copyOf(statements);
    }

    @Override
    public void run(Context context) {
        for (var statement : statements) {
            statement.run(context);
        }
    }

    @Override
    public SourceSpan getPos() {
        return null;
    }
}
