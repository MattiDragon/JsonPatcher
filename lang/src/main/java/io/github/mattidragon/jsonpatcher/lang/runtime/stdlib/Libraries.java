package io.github.mattidragon.jsonpatcher.lang.runtime.stdlib;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.function.PatchFunction;

import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;

/**
 * Contains standard libraries. They are built using reflection over public class members.
 * Private members are ignored and a zero argument public constructor is required.
 * Values of fields are placed directly into the library object, which methods are converted to functions first.
 * Method overloading is supported for differing argument counts only.
 */
@SuppressWarnings("unused")
public class Libraries {
    /**
     * Libraries that can be imported by the user.
     */
    public static final Map<String, Supplier<Value.ObjectValue>> LOOKUP = Map.of();
    /**
     * Libraries that are built in and always available.
     */
    public static final Map<String, Supplier<Value.ObjectValue>> BUILTIN = Map.of(
            "math", new LibraryBuilder(MathLibrary.class)::build,
            "arrays", new LibraryBuilder(ArraysLibrary.class)::build);

    public static class MathLibrary {
        public final Value.NumberValue PI = new Value.NumberValue(Math.PI);
        public final Value.NumberValue E = new Value.NumberValue(Math.E);

        // We use fields here because there are many similar methods
        public final Value.FunctionValue asin = numberUnary(Math::asin);
        public final Value.FunctionValue sin = numberUnary(Math::sin);
        public final Value.FunctionValue sinh = numberUnary(Math::sinh);
        public final Value.FunctionValue acos = numberUnary(Math::acos);
        public final Value.FunctionValue cos = numberUnary(Math::cos);
        public final Value.FunctionValue cosh = numberUnary(Math::cosh);
        public final Value.FunctionValue atan = numberUnary(Math::atan);
        public final Value.FunctionValue tan = numberUnary(Math::tan);
        public final Value.FunctionValue tanh = numberUnary(Math::tanh);
        public final Value.FunctionValue exp = numberUnary(Math::exp);
        public final Value.FunctionValue log = numberUnary(Math::log);
        public final Value.FunctionValue log10 = numberUnary(Math::log10);
        public final Value.FunctionValue sqrt = numberUnary(Math::sqrt);
        public final Value.FunctionValue cbrt = numberUnary(Math::cbrt);
        public final Value.FunctionValue ceil = numberUnary(Math::ceil);
        public final Value.FunctionValue floor = numberUnary(Math::floor);
        public final Value.FunctionValue abs = numberUnary(Math::abs);
        public final Value.FunctionValue signum = numberUnary(Math::signum);

        public Value.NumberValue max(Value.NumberValue first, Value.NumberValue second) {
            return new Value.NumberValue(Math.max(first.value(), second.value()));
        }

        public Value.NumberValue min(Value.NumberValue first, Value.NumberValue second) {
            return new Value.NumberValue(Math.min(first.value(), second.value()));
        }

        public Value.NumberValue pow(Value.NumberValue first, Value.NumberValue second) {
            return new Value.NumberValue(Math.pow(first.value(), second.value()));
        }

        private static Value.FunctionValue numberUnary(DoubleUnaryOperator operator) {
            return new Value.FunctionValue(((PatchFunction.BuiltInPatchFunction) (context, args, callPos) -> {
                if (!(args.get(0) instanceof Value.NumberValue value)) {
                    throw new EvaluationException("Expected argument to be number, was %s".formatted(args.get(0)), callPos);
                }
                return new Value.NumberValue(operator.applyAsDouble(value.value()));
            }).argCount(1));
        }
    }

    public static class ArraysLibrary {
        public Value.ArrayValue insert(LibraryBuilder.FunctionContext context, Value.ArrayValue array, Value.NumberValue index, Value value) {
            array.value().add(fixIndexForInsert((int) index.value(), array.value().size(), context.callPos()), value);
            return array;
        }

        // Can't use same algorithm as setting, because you can also add at the end.
        private static int fixIndexForInsert(int index, int size, SourceSpan pos) {
            if (index > size || index < -size)
                throw new EvaluationException("Array index out of bounds (index: %s, size: %s)".formatted(index, size), pos);
            if (index < 0) return size + index + 1; // Offset needed for -1 to mean last element
            return index;
        }
    }
}
