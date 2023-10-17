package io.github.mattidragon.jsonpatcher.lang.runtime.statement;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.expression.Expression;

public record ForEachLoopStatement(Expression iterable, String variableName, Statement body, SourceSpan pos) implements Statement {
    @Override
    public void run(EvaluationContext context) {
        var values = iterable.evaluate(context);
        if (!(values instanceof Value.ArrayValue arrayValue)) {
            throw new EvaluationException("Can only iterate arrays, tried to iterate %s".formatted(values), iterable.getPos());
        }
        for (var value : arrayValue.value()) {
            var loopContext = context.newScope();
            loopContext.variables().createVariable(variableName, value, false, pos);
            try {
                body.run(context);
            } catch (BreakStatement.BreakException e) {
                break;
            } catch (ContinueStatement.ContinueException e) {
                // Continue
            }
        }
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
