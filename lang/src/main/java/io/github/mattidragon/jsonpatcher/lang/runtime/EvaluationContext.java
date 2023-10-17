package io.github.mattidragon.jsonpatcher.lang.runtime;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.stdlib.Libraries;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashSet;
import java.util.Set;

public record EvaluationContext(Value.ObjectValue root, VariableStack variables, LibraryLocator libraryLocator) {
    private static final ThreadLocal<Set<String>> LIBRARY_RECURSION_DETECTOR = ThreadLocal.withInitial(HashSet::new);

    public static EvaluationContext create(Value.ObjectValue json, LibraryLocator libraryLocator) {
        var variables = new VariableStack();
        var context = new EvaluationContext(json, variables, libraryLocator);
        Libraries.BUILTIN.forEach((name, supplier) -> variables.createVariable(name, supplier.get(), false, null));
        return context.newScope();
    }

    public EvaluationContext withRoot(Value.ObjectValue root) {
        return new EvaluationContext(root, variables, libraryLocator);
    }

    public EvaluationContext newScope() {
        return new EvaluationContext(root, new VariableStack(variables), libraryLocator);
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
}
