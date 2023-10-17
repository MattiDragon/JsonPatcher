package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;

import java.util.List;

public record BlockStatement(List<Statement> statements, SourceSpan pos) implements Statement {
    public BlockStatement {
        statements = List.copyOf(statements);
    }

    @Override
    public void run(EvaluationContext context) {
        context = context.newScope();
        for (var statement : statements) {
            statement.run(context);
        }
    }

    @Override
    public SourceSpan getPos() {
        return null;
    }
}
