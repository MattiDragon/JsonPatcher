package io.github.mattidragon.jsonpatcher.lang.runtime;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.stdlib.Libraries;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record EvaluationContext(Value.ObjectValue root, VariableStack variables, LibraryLocator libraryLocator, Consumer<Value> debugConsumer) {
    private static final ThreadLocal<Set<String>> LIBRARY_RECURSION_DETECTOR = ThreadLocal.withInitial(HashSet::new);

    public static Builder builder() {
        return new Builder();
    }

    public EvaluationContext withRoot(Value.ObjectValue root) {
        return new EvaluationContext(root, variables, libraryLocator, debugConsumer);
    }

    public EvaluationContext newScope() {
        return new EvaluationContext(root, new VariableStack(variables), libraryLocator, debugConsumer);
    }

    public void log(Value value) {
        debugConsumer.accept(value);
    }

    public Value findLibrary(String libraryName, SourceSpan pos) {
        if (Libraries.LOOKUP.containsKey(libraryName)) {
            return Libraries.LOOKUP.get(libraryName).get();
        }
        if (Libraries.BUILTIN.containsKey(libraryName)) {
            throw new EvaluationException("Cannot load builtin library %s. You don't need to import it.".formatted(libraryName), pos);
        }

        try {
            if (!LIBRARY_RECURSION_DETECTOR.get().add(libraryName)) {
                throw new EvaluationException("Recursive library import detected for %s".formatted(libraryName), pos);
            }
            var json = new Value.ObjectValue();
            libraryLocator.loadLibrary(libraryName, json, pos);
            return json;
        } finally {
            LIBRARY_RECURSION_DETECTOR.get().remove(libraryName);
        }
    }

    @FunctionalInterface
    public interface LibraryLocator {
        /**
         * Locates a library with the given name and puts it into the given object. All builtin libraries are already handled.
         * @param libraryName The name of the library to locate. This is the name given in the import statement. Implementation may choose any syntax they like for this.
         * @param libraryObject The object to load the library into. This object will be returned to the user.
         * @param importPos The position of the import statement. This can be used for error reporting.
         * @throws EvaluationException For any expected errors during loading, like a missing library or an error while calling it. This will give a nice stacktrace for the user.
         */
        @ApiStatus.OverrideOnly
        void loadLibrary(String libraryName, Value.ObjectValue libraryObject, SourceSpan importPos);
    }

    public static class Builder {
        private Value.ObjectValue root = new Value.ObjectValue();
        private final VariableStack variables = new VariableStack();
        private LibraryLocator libraryLocator = (name, obj, pos) -> {
            throw new EvaluationException("No libraries available", pos);
        };
        private Consumer<Value> debugConsumer = x -> System.out.println("Debug from patch: " + x);
        private Map<String, Supplier<Value.ObjectValue>> stdlib = Libraries.BUILTIN;

        public Builder root(Value.ObjectValue root) {
            this.root = root;
            return this;
        }

        public Builder variable(String name, String value) {
            return variable(name, new Value.StringValue(value));
        }

        public Builder variable(String name, boolean value) {
            return variable(name, Value.BooleanValue.of(value));
        }

        public Builder variable(String name, Value value) {
            variables.createVariable(name, value, false, null);
            return this;
        }

        public Builder libraryLocator(LibraryLocator libraryLocator) {
            this.libraryLocator = libraryLocator;
            return this;
        }

        public Builder debugConsumer(Consumer<Value> debugConsumer) {
            this.debugConsumer = debugConsumer;
            return this;
        }

        public Builder stdlib(Map<String, Supplier<Value.ObjectValue>> stdlib) {
            this.stdlib = stdlib;
            return this;
        }

        public EvaluationContext build() {
            stdlib.forEach((name, supplier) -> variables.createVariable(name, supplier.get(), false, null));
            return new EvaluationContext(root, variables, libraryLocator, debugConsumer).newScope();
        }
    }
}
