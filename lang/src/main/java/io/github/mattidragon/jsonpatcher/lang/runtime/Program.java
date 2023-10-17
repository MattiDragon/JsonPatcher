package io.github.mattidragon.jsonpatcher.lang.runtime;

import io.github.mattidragon.jsonpatcher.lang.runtime.function.ReturnException;
import io.github.mattidragon.jsonpatcher.lang.runtime.statement.Statement;

import java.util.List;

public record Program(List<Statement> statements) {
    public void execute(EvaluationContext context) {
        try {
            for (var statement : statements) {
                statement.run(context);
            }
        } catch (ReturnException ignored) {
            // Catch returns to allow top level return to end script
        }
    }
}
