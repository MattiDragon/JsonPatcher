package io.github.mattidragon.jsonpatcher.lang.runtime.stdlib;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.function.PatchFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
            "arrays", new LibraryBuilder(ArraysLibrary.class)::build,
            "strings", new LibraryBuilder(StringsLibrary.class)::build,
            "functions", new LibraryBuilder(FunctionsLibrary.class)::build,
            "debug", new LibraryBuilder(DebugLibrary.class)::build);

    public static class MathLibrary {
        public final Value.NumberValue PI = new Value.NumberValue(Math.PI);
        public final Value.NumberValue E = new Value.NumberValue(Math.E);
        public final Value.NumberValue NaN = new Value.NumberValue(Double.NaN);
        public final Value.NumberValue POSITIVE_INFINITY = new Value.NumberValue(Double.POSITIVE_INFINITY);
        public final Value.NumberValue NEGATIVE_INFINITY = new Value.NumberValue(Double.NEGATIVE_INFINITY);

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
        @DontBind
        public static final Map<String, PatchFunction.BuiltInPatchFunction> METHODS = new LibraryBuilder(ArraysLibrary.class, Method.class).getFunctions();

        @Method
        public Value.ArrayValue insert(LibraryBuilder.FunctionContext context, Value.ArrayValue array, Value.NumberValue index, Value value) {
            array.value().add(fixIndexForInsert((int) index.value(), array.value().size(), context.callPos()), value);
            return array;
        }

        @Method
        public Value.ArrayValue push(Value.ArrayValue array, Value value) {
            array.value().add(value);
            return array;
        }

        @Method
        public Value pop(LibraryBuilder.FunctionContext context, Value.ArrayValue array) {
            if (array.value().isEmpty()) throw new EvaluationException("Can't pop from empty array", context.callPos());
            return array.value().remove(array.value().size() - 1);
        }

        @Method
        public Value.ArrayValue remove(LibraryBuilder.FunctionContext context, Value.ArrayValue array, Value element) {
            array.value().remove(element);
            return array;
        }

        @Method
        public Value removeAt(LibraryBuilder.FunctionContext context, Value.ArrayValue array, Value.NumberValue index) {
            var i = (int) index.value();
            var found = array.get(i, context.callPos());
            array.remove(i, context.callPos());
            return found;
        }

        @Method
        public Value.ArrayValue map(LibraryBuilder.FunctionContext context, Value.ArrayValue array, Value.FunctionValue function) {
            var newArray = new Value.ArrayValue();
            for (var value : array.value()) {
                newArray.value().add(function.function().execute(context.context(), List.of(value), context.callPos()));
            }
            return newArray;
        }

        @Method
        public Value.ArrayValue mapped(LibraryBuilder.FunctionContext context, Value.ArrayValue array, Value.FunctionValue function) {
            for (int i = 0; i < array.value().size(); i++) {
                array.value().set(i, function.function().execute(context.context(), List.of(array.get(i, context.callPos())), context.callPos()));
            }
            return array;
        }

        @Method
        public Value.ArrayValue filter(LibraryBuilder.FunctionContext context, Value.ArrayValue array, Value.FunctionValue function) {
            var newArray = new Value.ArrayValue();
            for (var value : array.value()) {
                var result = function.function().execute(context.context(), List.of(value), context.callPos());
                if (result.asBoolean()) newArray.value().add(value);
            }
            return newArray;
        }

        @Method
        public Value.ArrayValue filtered(LibraryBuilder.FunctionContext context, Value.ArrayValue array, Value.FunctionValue function) {
            array.value().removeIf(value -> !function.function().execute(context.context(), List.of(value), context.callPos()).asBoolean());
            return array;
        }

        @Method
        public Value reduce(LibraryBuilder.FunctionContext context, Value.ArrayValue array, Value.FunctionValue function, Value initialValue) {
            var result = initialValue;
            for (var value : array.value()) {
                result = function.function().execute(context.context(), List.of(result, value), context.callPos());
            }
            return result;
        }

        @Method
        public Value.ArrayValue slice(LibraryBuilder.FunctionContext context, Value.ArrayValue array, Value.NumberValue start, Value.NumberValue end) {
            var newArray = new Value.ArrayValue();
            var s = (int) start.value();
            var e = (int) end.value();
            if (s < 0 || s > array.value().size()) throw new EvaluationException("Array index out of bounds (index: %s, size: %s)".formatted(s, array.value().size()), context.callPos());
            if (e < 0 || e > array.value().size()) throw new EvaluationException("Array index out of bounds (index: %s, size: %s)".formatted(e, array.value().size()), context.callPos());
            if (s > e) throw new EvaluationException("Start index must be less than end index (start: %s, end: %s)".formatted(s, e), context.callPos());
            for (int i = s; i < e; i++) {
                newArray.value().add(array.value().get(i));
            }
            return newArray;
        }

        @Method
        public Value.ArrayValue slice(LibraryBuilder.FunctionContext context, Value.ArrayValue array, Value.NumberValue start) {
            var newArray = new Value.ArrayValue();
            var s = (int) start.value();
            if (s < 0 || s > array.value().size()) throw new EvaluationException("Array index out of bounds (index: %s, size: %s)".formatted(s, array.value().size()), context.callPos());
            for (int i = s; i < array.value().size(); i++) {
                newArray.value().add(array.value().get(i));
            }
            return newArray;
        }

        // Can't use same algorithm as normal access, because you can also add at the end.
        @DontBind
        private static int fixIndexForInsert(int index, int size, SourceSpan pos) {
            if (index > size || index < -size)
                throw new EvaluationException("Array index out of bounds (index: %s, size: %s)".formatted(index, size), pos);
            if (index < 0) return size + index + 1; // Offset needed for -1 to mean last element
            return index;
        }
    }

    public static class StringsLibrary {
        @DontBind
        public static final Map<String, PatchFunction.BuiltInPatchFunction> METHODS = new LibraryBuilder(StringsLibrary.class, Method.class).getFunctions();

        @Method
        public Value.StringValue replace(LibraryBuilder.FunctionContext context, Value.StringValue string, Value.StringValue pattern, Value.StringValue replacement) {
            return new Value.StringValue(string.value().replace(pattern.value(), replacement.value()));
        }

        @Method
        public Value.StringValue replaceRegex(LibraryBuilder.FunctionContext context, Value.StringValue string, Value.StringValue pattern, Value.StringValue replacement) {
            return new Value.StringValue(string.value().replaceAll(pattern.value(), replacement.value()));
        }

        @Method
        public Value.ArrayValue split(LibraryBuilder.FunctionContext context, Value.StringValue string, Value.StringValue pattern) {
            var array = new Value.ArrayValue();
            for (var part : string.value().split(pattern.value())) {
                array.value().add(new Value.StringValue(part));
            }
            return array;
        }

        @Method
        public Value.StringValue toLowerCase(LibraryBuilder.FunctionContext context, Value.StringValue string) {
            return new Value.StringValue(string.value().toLowerCase(Locale.ROOT));
        }

        @Method
        public Value.StringValue toUpperCase(LibraryBuilder.FunctionContext context, Value.StringValue string) {
            return new Value.StringValue(string.value().toUpperCase(Locale.ROOT));
        }

        @Method
        public Value.StringValue trim(LibraryBuilder.FunctionContext context, Value.StringValue string) {
            return new Value.StringValue(string.value().strip());
        }

        @Method
        public Value.StringValue trimStart(LibraryBuilder.FunctionContext context, Value.StringValue string) {
            return new Value.StringValue(string.value().stripLeading());
        }

        @Method
        public Value.StringValue trimEnd(LibraryBuilder.FunctionContext context, Value.StringValue string) {
            return new Value.StringValue(string.value().stripTrailing());
        }

        @Method
        public Value.BooleanValue startsWith(LibraryBuilder.FunctionContext context, Value.StringValue string, Value.StringValue prefix) {
            return Value.BooleanValue.of(string.value().startsWith(prefix.value()));
        }

        @Method
        public Value.BooleanValue endsWith(LibraryBuilder.FunctionContext context, Value.StringValue string, Value.StringValue suffix) {
            return Value.BooleanValue.of(string.value().endsWith(suffix.value()));
        }

        @Method
        public Value.BooleanValue contains(LibraryBuilder.FunctionContext context, Value.StringValue string, Value.StringValue substring) {
            return Value.BooleanValue.of(string.value().contains(substring.value()));
        }

        @Method
        public Value.NumberValue length(LibraryBuilder.FunctionContext context, Value.StringValue string) {
            return new Value.NumberValue(string.value().length());
        }

        @Method
        public Value.BooleanValue isEmpty(LibraryBuilder.FunctionContext context, Value.StringValue string) {
            return Value.BooleanValue.of(string.value().isEmpty());
        }

        @Method
        public Value.BooleanValue isBlank(LibraryBuilder.FunctionContext context, Value.StringValue string) {
            return Value.BooleanValue.of(string.value().isBlank());
        }

        @Method
        public Value.StringValue charAt(LibraryBuilder.FunctionContext context, Value.StringValue string, Value.NumberValue index) {
            var i = (int) index.value();
            if (i < 0 || i >= string.value().length()) throw new EvaluationException("String index out of bounds (index: %s, size: %s)".formatted(i, string.value().length()), context.callPos());
            return new Value.StringValue(string.value().substring(i, i + 1));
        }

        @Method
        public Value.ArrayValue chars(LibraryBuilder.FunctionContext context, Value.StringValue string) {
            var array = new Value.ArrayValue();
            for (var c : string.value().toCharArray()) {
                array.value().add(new Value.StringValue(String.valueOf(c)));
            }
            return array;
        }

        @Method
        public Value.StringValue substring(LibraryBuilder.FunctionContext context, Value.StringValue string, Value.NumberValue start, Value.NumberValue end) {
            var s = (int) start.value();
            var e = (int) end.value();
            if (s < 0 || s > string.value().length()) throw new EvaluationException("String index out of bounds (index: %s, size: %s)".formatted(s, string.value().length()), context.callPos());
            if (e < 0 || e > string.value().length()) throw new EvaluationException("String index out of bounds (index: %s, size: %s)".formatted(e, string.value().length()), context.callPos());
            if (s > e) throw new EvaluationException("Start index must be less than end index (start: %s, end: %s)".formatted(s, e), context.callPos());
            return new Value.StringValue(string.value().substring(s, e));
        }

        @Method
        public Value.StringValue substring(LibraryBuilder.FunctionContext context, Value.StringValue string, Value.NumberValue start) {
            var s = (int) start.value();
            if (s < 0 || s > string.value().length()) throw new EvaluationException("String index out of bounds (index: %s, size: %s)".formatted(s, string.value().length()), context.callPos());
            return new Value.StringValue(string.value().substring(s));
        }

        public Value.StringValue join(LibraryBuilder.FunctionContext context, Value.ArrayValue array, Value.StringValue separator) {
            var builder = new StringBuilder();
            var first = true;
            for (var value : array.value()) {
                if (!first) builder.append(separator.value());
                first = false;
                builder.append(asString(context, value));
            }
            return new Value.StringValue(builder.toString());
        }

        public Value asString(LibraryBuilder.FunctionContext context, Value value) {
            if (value instanceof Value.StringValue string) {
                return string;
            }
            if (value instanceof Value.NumberValue number) {
                return new Value.StringValue(String.valueOf(number.value()));
            }
            if (value instanceof Value.BooleanValue bool) {
                return new Value.StringValue(String.valueOf(bool.value()));
            }
            if (value instanceof Value.NullValue) {
                return new Value.StringValue("null");
            }
            if (value instanceof Value.ArrayValue array) {
                var builder = new StringBuilder();
                builder.append('[');
                for (int i = 0; i < array.value().size(); i++) {
                    if (i != 0) builder.append(", ");
                    builder.append(asString(context, array.value().get(i)));
                }
                builder.append(']');
                return new Value.StringValue(builder.toString());
            }
            if (value instanceof Value.ObjectValue object) {
                var builder = new StringBuilder();
                builder.append('{');
                var first = true;
                for (var entry : object.value().entrySet()) {
                    if (!first) builder.append(", ");
                    first = false;
                    builder.append(entry.getKey()).append(": ").append(asString(context, entry.getValue()));
                }
                builder.append('}');
                return new Value.StringValue(builder.toString());
            }
            throw new EvaluationException("Can't convert %s to string".formatted(value), context.callPos());
        }
    }

    public static class FunctionsLibrary {
        @DontBind
        public static final Map<String, PatchFunction.BuiltInPatchFunction> METHODS = new LibraryBuilder(FunctionsLibrary.class, Method.class).getFunctions();

        @Method
        public static Value.FunctionValue bind(Value.FunctionValue function, Value value) {
            return new Value.FunctionValue((PatchFunction.BuiltInPatchFunction) (context, args, callPos) -> {
                var newArgs = new ArrayList<>(args);
                newArgs.add(0, value);
                return function.function().execute(context, newArgs, callPos);
            });
        }

        @Method
        public Value.FunctionValue bind(Value.FunctionValue function, Value value, Value.NumberValue index) {
            return new Value.FunctionValue((PatchFunction.BuiltInPatchFunction) (context1, args, callPos) -> {
                var newArgs = new ArrayList<>(args);
                newArgs.add((int) index.value(), value);
                return function.function().execute(context1, newArgs, callPos);
            });
        }

        @Method
        public Value.FunctionValue then(Value.FunctionValue function, Value.FunctionValue next) {
            return new Value.FunctionValue(((PatchFunction.BuiltInPatchFunction) (context1, args, callPos) -> {
                var result = function.function().execute(context1, args, callPos);
                return next.function().execute(context1, List.of(result), callPos);
            }));
        }


        public Value.FunctionValue identity() {
            return new Value.FunctionValue(((PatchFunction.BuiltInPatchFunction) (context, args, callPos) -> args.get(0)).argCount(1));
        }

        public Value.FunctionValue constant(Value value) {
            return new Value.FunctionValue(((PatchFunction.BuiltInPatchFunction) (context, args, callPos) -> value).argCount(0));
        }
    }

    public static class DebugLibrary {
        public void log(LibraryBuilder.FunctionContext context, Value value) {
            context.context().log(value);
        }
    }
}
