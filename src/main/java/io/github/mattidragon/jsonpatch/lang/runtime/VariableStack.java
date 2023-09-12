package io.github.mattidragon.jsonpatch.lang.runtime;

import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Objects;

public final class VariableStack {
    private final @Nullable VariableStack parent;
    private final HashMap<String, Value> mutable;
    private final HashMap<String, Value> immutable;

    public VariableStack() {
        this(null);
    }

    public VariableStack(@Nullable VariableStack parent) {
        this.parent = parent;
        this.mutable = new HashMap<>();
        this.immutable = new HashMap<>();
    }

    public Value get(String name, SourceSpan pos) {
        if (mutable.containsKey(name)) {
            return mutable.get(name);
        }
        if (immutable.containsKey(name)) {
            return immutable.get(name);
        }
        if (parent != null) {
            return parent.get(name, pos);
        }
        throw new EvaluationException("Cannot find variable with name %s".formatted(name), pos);
    }

    private boolean has(String name) {
        if (mutable.containsKey(name)) return true;
        if (immutable.containsKey(name)) return true;
        if (parent != null) return parent.has(name);
        return false;
    }

    public void set(String name, Value value, SourceSpan pos) {
        if (parent != null && parent.has(name)) parent.set(name, value, pos);
        if (mutable.containsKey(name)) {
            mutable.put(name, value);
        } else if (immutable.containsKey(name)) {
            throw new EvaluationException("Attempt to assign to mutable variable $%s".formatted(name), pos);
        } else {
            throw new EvaluationException("Cannot find variable with name $%s".formatted(name), pos);
        }
    }

    public void create(String name, Value value, boolean mutable, SourceSpan pos) {
        if (has(name)) throw new EvaluationException("Cannot create variable with duplicate name: $%s".formatted(name), pos);
        if (mutable) this.mutable.put(name, value);
        else this.immutable.put(name, value);
    }

    public void delete(String name, SourceSpan pos) {
        if (immutable.containsKey(name)) {
            immutable.remove(name);
            return;
        }
        if (mutable.containsKey(name)) {
            mutable.remove(name);
            return;
        }
        if (has(name)) throw new EvaluationException("Cannot delete variable from outer scope: $%s".formatted(name), pos);
        throw new EvaluationException("Cannot find variable with name %s".formatted(name), pos);
    }
}
