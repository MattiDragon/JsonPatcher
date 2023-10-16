package io.github.mattidragon.jsonpatcher;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.VariableStack;
import io.github.mattidragon.jsonpatcher.lang.runtime.stdlib.Libraries;
import io.github.mattidragon.jsonpatcher.patch.PatchContext;
import io.github.mattidragon.jsonpatcher.patch.PatchStorage;
import net.minecraft.util.Identifier;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public record ContextImpl(Value.ObjectValue root, VariableStack variables, PatchStorage storage) implements Context {
    private static final ThreadLocal<Deque<Identifier>> LIBRARY_RECURSION_DETECTOR = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<ExecutorService> LIBRARY_APPLICATOR = ThreadLocal.withInitial(() -> Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("JsonPatch Library Builder (%s)").build()));

    public ContextImpl withRoot(Value.ObjectValue root) {
        return new ContextImpl(root, variables, storage);
    }

    public ContextImpl newScope() {
        return new ContextImpl(root, new VariableStack(variables), storage);
    }

    public static ContextImpl create(Value.ObjectValue json, PatchStorage storage) {
        var variables = new VariableStack();
        return new ContextImpl(json, variables, storage);
    }

    public Value findLibrary(String libraryName, SourceSpan pos) {
        if (Libraries.LOOKUP.containsKey(libraryName)) {
            return Libraries.LOOKUP.get(libraryName).get();
        }
        var libId = Identifier.tryParse(libraryName);
        var userLib = storage.findLibrary(libId);

        if (userLib == null || libId == null) {
            throw new EvaluationException("Cannot locate library '%s'".formatted(libraryName), pos);
        }

        try {
            LIBRARY_RECURSION_DETECTOR.get().add(libId);
            var json = new Value.ObjectValue();
            PatchContext.runPatch(userLib, LIBRARY_APPLICATOR.get(), e -> {
                if (e instanceof EvaluationException evaluationException) {
                    throw new EvaluationException("Failed to load library %s".formatted(libId), pos, evaluationException);
                } else {
                    throw new RuntimeException("Failed to load library %s".formatted(libId), e);
                }
            }, storage, json);
            JsonPatcher.RELOAD_LOGGER.debug("Loaded library {} (current stack: {})", libId, LIBRARY_RECURSION_DETECTOR.get());
            return json;
        } finally {
            if (libId.equals(LIBRARY_RECURSION_DETECTOR.get().peek())) {
                LIBRARY_RECURSION_DETECTOR.get().pop();
            }
        }
    }

    @Override
    public String toString() {
        return "Context[]";
    }
}
