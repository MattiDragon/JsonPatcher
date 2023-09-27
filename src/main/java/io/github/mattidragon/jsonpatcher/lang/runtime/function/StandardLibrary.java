package io.github.mattidragon.jsonpatcher.lang.runtime.function;

import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static io.github.mattidragon.jsonpatcher.lang.runtime.function.PatchFunction.BuiltInPatchFunction.numberUnary;

public class StandardLibrary implements Supplier<Value.ObjectValue> {
    public static final Map<String, Supplier<Value.ObjectValue>> LIBRARIES = Map.of(
            "math", math(),
            "arrays", arrays()
    );

    private final Map<String, PatchFunction> functions = new LinkedHashMap<>();

    private static StandardLibrary math() {
        return new StandardLibrary()
                .func("asin", numberUnary(Math::asin))
                .func("sin", numberUnary(Math::sin))
                .func("sinh", numberUnary(Math::sinh))
                .func("acos", numberUnary(Math::acos))
                .func("cos", numberUnary(Math::cos))
                .func("cosh", numberUnary(Math::cosh))
                .func("atan", numberUnary(Math::atan))
                .func("tan", numberUnary(Math::tan))
                .func("tanh", numberUnary(Math::tanh))
                .func("exp", numberUnary(Math::exp))
                .func("log", numberUnary(Math::log))
                .func("log10", numberUnary(Math::log10))
                .func("sqrt", numberUnary(Math::sqrt))
                .func("cbrt", numberUnary(Math::cbrt))
                .func("ceil", numberUnary(Math::ceil))
                .func("floor", numberUnary(Math::floor))
                .func("abs", numberUnary(Math::abs))
                .func("signum", numberUnary(Math::signum));
    }

    private static StandardLibrary arrays() {
        return new StandardLibrary()
                .func("insert", (context, args, callPos) -> {
                    if (!(args.get(0) instanceof Value.ArrayValue array)) throw new EvaluationException("Expected argument 0 to be array, was %s".formatted(args.get(0)), callPos);;
                    if (!(args.get(1) instanceof Value.NumberValue index)) throw new EvaluationException("Expected argument 1 to be number, was %s".formatted(args.get(1)), callPos);;
                    array.value().add((int) index.value(), args.get(2));
                    return array;
                }, 3);
    }

    private StandardLibrary func(String name, PatchFunction.BuiltInPatchFunction function) {
        functions.put(name, function);
        return this;
    }

    private StandardLibrary func(String name, PatchFunction.BuiltInPatchFunction function, int argCount) {
        functions.put(name, function.argCount(argCount));
        return this;
    }

    @Override
    public Value.ObjectValue get() {
        var object = new Value.ObjectValue();
        functions.forEach((name, function) -> object.value().put(name, new Value.FunctionValue(function)));
        return object;
    }
}
