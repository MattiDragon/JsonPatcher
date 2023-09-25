package io.github.mattidragon.jsonpatch.lang.runtime;

import io.github.mattidragon.jsonpatch.lang.runtime.function.BuiltInFunctions;

public record Context(Value.ObjectValue root, VariableStack variables) {
    public Context withRoot(Value.ObjectValue root) {
        return new Context(root, variables);
    }

    public Context newScope() {
        return new Context(root, new VariableStack(variables));
    }

    public static Context create(Value.ObjectValue json) {
        return new Context(json,
                new VariableStack(BuiltInFunctions.ALL_FUNCTIONS));
    }
}
