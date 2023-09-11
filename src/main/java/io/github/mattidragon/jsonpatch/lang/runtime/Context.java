package io.github.mattidragon.jsonpatch.lang.runtime;

public record Context(Value.ObjectValue root, VariableStack variables) {
    public Context withRoot(Value.ObjectValue root) {
        return new Context(root, variables);
    }

    public Context newScope() {
        return new Context(root, new VariableStack(variables));
    }
}
