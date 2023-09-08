package io.github.mattidragon.jsonpatch.lang.ast;

public record Context(Value.ObjectValue root) {
    public Context withRoot(Value.ObjectValue root) {
        return new Context(root);
    }
}
