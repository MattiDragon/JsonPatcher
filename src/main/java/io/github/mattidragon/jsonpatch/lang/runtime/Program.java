package io.github.mattidragon.jsonpatch.lang.runtime;

import io.github.mattidragon.jsonpatch.lang.runtime.function.ReturnException;
import io.github.mattidragon.jsonpatch.lang.runtime.statement.Statement;

import java.util.List;

public record Program(List<Statement> statements) {
    public void execute(Context context) {
        try {
            for (var statement : statements) {
                statement.run(context);
            }
        } catch (ReturnException ignored) {
            // Catch returns to allow top level return to end script
        }
    }
}
