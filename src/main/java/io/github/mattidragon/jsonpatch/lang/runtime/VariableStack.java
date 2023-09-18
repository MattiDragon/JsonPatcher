package io.github.mattidragon.jsonpatch.lang.runtime;

import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatch.lang.runtime.function.PatchFunction;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class VariableStack {
    private final @Nullable VariableStack parent;
    private final HashMap<String, Value> mutable = new HashMap<>();
    private final HashMap<String, Value> immutable = new HashMap<>();
    private final HashMap<String, PatchFunction> functions;

    public VariableStack(Map<String, ? extends PatchFunction> functions) {
        this.parent = null;
        this.functions = new HashMap<>(functions);
    }

    public VariableStack(@Nullable VariableStack parent) {
        this.parent = parent;
        this.functions = new HashMap<>();
    }

    public Value getVariable(String name, SourceSpan pos) {
        if (mutable.containsKey(name)) {
            return mutable.get(name);
        }
        if (immutable.containsKey(name)) {
            return immutable.get(name);
        }
        if (parent != null) {
            return parent.getVariable(name, pos);
        }
        throw new EvaluationException("Cannot find variable with name %s".formatted(name), pos);
    }

    private boolean hasVariable(String name) {
        if (mutable.containsKey(name)) return true;
        if (immutable.containsKey(name)) return true;
        if (parent != null) return parent.hasVariable(name);
        return false;
    }

    public void setVariable(String name, Value value, SourceSpan pos) {
        if (parent != null && parent.hasVariable(name)) parent.setVariable(name, value, pos);
        if (mutable.containsKey(name)) {
            mutable.put(name, value);
        } else if (immutable.containsKey(name)) {
            throw new EvaluationException("Attempt to assign to mutable variable $%s".formatted(name), pos);
        } else {
            throw new EvaluationException("Cannot find variable with name $%s".formatted(name), pos);
        }
    }

    public void createVariable(String name, Value value, boolean mutable, SourceSpan pos) {
        if (hasVariable(name)) throw new EvaluationException("Cannot create variable with duplicate name: $%s".formatted(name), pos);
        if (mutable) this.mutable.put(name, value);
        else this.immutable.put(name, value);
    }

    public void createVariableWithShadowing(String name, Value value, boolean mutable) {
        if (mutable) this.mutable.put(name, value);
        else this.immutable.put(name, value);
    }

    public void deleteVariable(String name, SourceSpan pos) {
        if (immutable.containsKey(name)) {
            immutable.remove(name);
            return;
        }
        if (mutable.containsKey(name)) {
            mutable.remove(name);
            return;
        }
        if (hasVariable(name)) throw new EvaluationException("Cannot delete variable from outer scope: $%s".formatted(name), pos);
        throw new EvaluationException("Cannot find variable with name %s".formatted(name), pos);
    }

    private boolean hasFunction(String name) {
        if (functions.containsKey(name)) return true;
        if (parent != null) return parent.hasFunction(name);
        return false;
    }

    public void defineFunction(String name, PatchFunction function, SourceSpan pos) {
        if (hasFunction(name)) throw new EvaluationException("Cannot create function with duplicate name: %s".formatted(name), pos);
        functions.put(name, function);
    }

    public PatchFunction getFunction(String name, SourceSpan pos) {
        if (functions.containsKey(name)) return functions.get(name);
        if (parent != null) return parent.getFunction(name, pos);
        throw new EvaluationException("Cannot find function with name %s".formatted(name), pos);
    }
}
