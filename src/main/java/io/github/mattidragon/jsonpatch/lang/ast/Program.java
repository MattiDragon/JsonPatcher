package io.github.mattidragon.jsonpatch.lang.ast;

import java.util.List;

public record Program(List<Statement> statements) {
    public void execute(Context context) {
        for (var statement : statements) {
            statement.run(context);
        }
    }
}
