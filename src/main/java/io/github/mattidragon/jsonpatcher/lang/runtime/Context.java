package io.github.mattidragon.jsonpatcher.lang.runtime;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.function.BuiltInFunctions;
import io.github.mattidragon.jsonpatcher.lang.runtime.function.StandardLibrary;

public record Context(Value.ObjectValue root, VariableStack variables) {
    public Context withRoot(Value.ObjectValue root) {
        return new Context(root, variables);
    }

    public Context newScope() {
        return new Context(root, new VariableStack(variables));
    }

    public static Context create(Value.ObjectValue json) {
        var variables = new VariableStack();
        BuiltInFunctions.ALL_FUNCTIONS.forEach((name, func) -> variables.createVariable(name, new Value.FunctionValue(func), true, null));
        return new Context(json, variables);
    }

    public Value findLibrary(String libraryName, SourceSpan pos) {
        if (StandardLibrary.LIBRARIES.containsKey(libraryName)) {
            return StandardLibrary.LIBRARIES.get(libraryName).get();
        } else {
            throw new EvaluationException("Cannot locate library '%s'".formatted(libraryName), pos);
        }
    }
}
