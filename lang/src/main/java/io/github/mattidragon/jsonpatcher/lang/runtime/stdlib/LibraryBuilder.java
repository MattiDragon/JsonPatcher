package io.github.mattidragon.jsonpatcher.lang.runtime.stdlib;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.function.PatchFunction;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LibraryBuilder {
    private final Class<?> libraryClass;
    private final Object instance;
    private final HashMap<String, PatchFunction.BuiltInPatchFunction> functions = new HashMap<>();
    private final HashMap<String, Value> constants = new HashMap<>();
    private final Class<? extends Annotation> filterAnnotation;

    public LibraryBuilder(Class<?> libraryClass) {
        this(libraryClass, null);
    }

    public LibraryBuilder(Class<?> libraryClass, Class<? extends Annotation> filterAnnotation) {
        this.libraryClass = libraryClass;
        this.filterAnnotation = filterAnnotation;
        try {
            this.instance = libraryClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Failed to build library object", e);
        }
        buildFunctions();
        buildConstants();
    }

    private void buildFunctions() {
        var methods = findMethods();

        methods.forEach((name, overloads) -> {
            var byArgCount = groupOverloadsByArgCount(name, overloads);

            functions.put(name, (context, args, callPos) -> {
                var overload = byArgCount.get(args.size());
                if (overload == null) throw new EvaluationException("No overload of %s with %s arguments".formatted(name, args.size()), callPos);

                var hasContext = overload.getParameterTypes()[0] == FunctionContext.class;

                for (int i = 0; i < args.size(); i++) {
                    var arg = args.get(i);
                    var param = overload.getParameterTypes()[hasContext ? i + 1 : i];
                    if (!param.isInstance(arg)) throw new EvaluationException("Expected argument %s to be %s, was %s".formatted(i, param, arg), callPos);
                }

                try {
                    Object[] argsArray;
                    if (hasContext) {
                        argsArray = new Object[args.size() + 1];
                        System.arraycopy(args.toArray(), 0, argsArray, 1, args.size());
                        argsArray[0] = new FunctionContext(context, callPos);
                    } else {
                        argsArray = args.toArray();
                    }

                    var result = overload.invoke(instance, argsArray);

                    if (result == null) return Value.NullValue.NULL;
                    if (result instanceof Value value) return value;
                    throw new IllegalStateException("Unexpected return value from library function %s: %s".formatted(name, result));
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof EvaluationException e1) {
                        throw new EvaluationException("Error while calling builtin function %s: %s".formatted(name, e1.getMessage()), callPos);
                    } else {
                        throw new RuntimeException("Unexpected error while calling builtin function %s".formatted(name), e);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Library function %s is not accessible".formatted(name), e);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("Library function argument checks failed for %s".formatted(name), e);
                }
            });
        });
    }

    private static HashMap<Integer, Method> groupOverloadsByArgCount(String name, List<Method> overloads) {
        var byArgCount = new HashMap<Integer, Method>();
        for (var overload : overloads) {
            var argCount = overload.getParameterCount();
            if (argCount >= 1 && overload.getParameterTypes()[0] == FunctionContext.class) {
                argCount--;
            }

            if (byArgCount.containsKey(argCount)) {
                throw new IllegalStateException("Library function %s has multiple overloads with the same number of arguments".formatted(name));
            }
            byArgCount.put(argCount, overload);
        }
        return byArgCount;
    }

    private HashMap<String, List<Method>> findMethods() {
        var methods = new HashMap<String, List<Method>>();

        for (var method : libraryClass.getDeclaredMethods()) {
            if ((method.getModifiers() & Modifier.PUBLIC) == 0) continue;
            if (method.getAnnotation(DontBind.class) != null) continue;
            if (filterAnnotation != null && method.getAnnotation(filterAnnotation) == null) continue;
            if (method.isVarArgs()) throw new IllegalStateException("Library functions cannot be varargs (yet), %s is".formatted(method));

            var parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                var type = parameterTypes[i];

                // Allow context as first argument
                if (i == 0 && type == FunctionContext.class) {
                    continue;
                }

                if (!Value.class.isAssignableFrom(type)) {
                    throw new IllegalStateException("Library function parameters must be of subclass Value, %s from %s is not".formatted(type, method));
                }
            }

            if (method.getReturnType() != void.class && !Value.class.isAssignableFrom(method.getReturnType())) {
                throw new IllegalStateException("Library function return type must be of subclass Value or void, %s from %s is not".formatted(method.getReturnType(), method));
            }

            var methodName = method.getName();
            var nameOverride = method.getAnnotation(FunctionName.class);
            if (nameOverride != null) methodName = nameOverride.value();

            methods.computeIfAbsent(methodName, name -> new ArrayList<>()).add(method);
        }
        return methods;
    }

    private void buildConstants() {
        for (var field : libraryClass.getDeclaredFields()) {
            if ((field.getModifiers() & Modifier.PUBLIC) == 0) continue;
            if (field.getAnnotation(DontBind.class) != null) continue;
            if (filterAnnotation != null && field.getAnnotation(filterAnnotation) == null) continue;
            if (!Value.class.isAssignableFrom(field.getType())) throw new IllegalStateException("Library constants must be of subclass Value, %s from %s is not".formatted(field.getType(), field));
            try {
                constants.put(field.getName(), (Value) field.get(instance));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Library constant %s is not accessible".formatted(field), e);
            }
        }
    }

    public HashMap<String, PatchFunction.BuiltInPatchFunction> getFunctions() {
        return functions;
    }

    public Value.ObjectValue build() {
        var object = new Value.ObjectValue();
        functions.forEach((name, function) -> object.value().put(name, new Value.FunctionValue(function)));
        constants.forEach(object.value()::put);
        return object;
    }

    public record FunctionContext(EvaluationContext context, SourceSpan callPos) {
    }
}
