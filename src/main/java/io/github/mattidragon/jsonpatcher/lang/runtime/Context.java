package io.github.mattidragon.jsonpatcher.lang.runtime;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonObject;
import io.github.mattidragon.jsonpatcher.JsonPatcher;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.function.BuiltInFunctions;
import io.github.mattidragon.jsonpatcher.lang.runtime.function.StandardLibrary;
import io.github.mattidragon.jsonpatcher.patch.PatchContext;
import io.github.mattidragon.jsonpatcher.patch.PatchStorage;
import net.minecraft.util.Identifier;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public record Context(Value.ObjectValue root, VariableStack variables, PatchStorage storage) {
    private static final ThreadLocal<Deque<Identifier>> LIBRARY_RECURSION_DETECTOR = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<ExecutorService> LIBRARY_APPLICATOR = ThreadLocal.withInitial(() -> Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("JsonPatch Library Builder (%s)").build()));

    public Context withRoot(Value.ObjectValue root) {
        return new Context(root, variables, storage);
    }

    public Context newScope() {
        return new Context(root, new VariableStack(variables), storage);
    }

    public static Context create(Value.ObjectValue json, PatchStorage storage) {
        var variables = new VariableStack();
        BuiltInFunctions.ALL_FUNCTIONS.forEach((name, func) -> variables.createVariable(name, new Value.FunctionValue(func), true, null));
        return new Context(json, variables, storage);
    }

    public Value findLibrary(String libraryName, SourceSpan pos) {
        if (StandardLibrary.LIBRARIES.containsKey(libraryName)) {
            return StandardLibrary.LIBRARIES.get(libraryName).get();
        }
        var libId = Identifier.tryParse(libraryName);
        var userLib = storage.findLibrary(libId);

        if (userLib == null || libId == null) {
            throw new EvaluationException("Cannot locate library '%s'".formatted(libraryName), pos);
        }

        try {
            LIBRARY_RECURSION_DETECTOR.get().add(libId);
            var json = new Value.ObjectValue(new JsonObject());
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
}
