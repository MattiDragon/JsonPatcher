package io.github.mattidragon.jsonpatch.lang.runtime;

public record Context(Value.ObjectValue root) {
    public Context withRoot(Value.ObjectValue root) {
        return new Context(root);
    }
}
