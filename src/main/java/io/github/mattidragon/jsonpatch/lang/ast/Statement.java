package io.github.mattidragon.jsonpatch.lang.ast;

import io.github.mattidragon.jsonpatch.lang.ast.expression.Expression;
import io.github.mattidragon.jsonpatch.lang.ast.expression.Reference;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;

import java.util.List;

public interface Statement {
    void run(Context context);
    SourceSpan getPos();

    default EvaluationException error(String message) {
        return new EvaluationException(message, getPos());
    }

    record Assignment(Reference ref, Expression val, SourceSpan pos) implements Statement {
        @Override
        public void run(Context context) {
            ref.set(context, val.evaluate(context));
        }

        @Override
        public SourceSpan getPos() {
            return null;
        }
    }

    record Block(List<Statement> statements, SourceSpan pos) implements Statement {
        public Block {
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

    record Apply(Expression root, Statement action, SourceSpan pos) implements Statement {
        @Override
        public void run(Context context) {
            var root = this.root.evaluate(context);
            if (!(root instanceof Value.ObjectValue objectValue)) {
                throw error("Only objects can be used in apply statements, tried to use %s".formatted(root));
            }
            action.run(context.withRoot(objectValue));
        }

        @Override
        public SourceSpan getPos() {
            return pos;
        }
    }
}
