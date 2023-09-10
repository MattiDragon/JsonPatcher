package io.github.mattidragon.jsonpatch.lang.ast.expression;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.mattidragon.jsonpatch.lang.ast.Context;
import io.github.mattidragon.jsonpatch.lang.ast.EvaluationException;
import io.github.mattidragon.jsonpatch.lang.ast.Value;
import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;

import java.util.function.BiPredicate;

public record BinaryExpression(Expression first, Expression second, Operator op, SourceSpan opPos) implements Expression {
    @Override
    public Value evaluate(Context context) {
        return op.apply(first.evaluate(context), second.evaluate(context), opPos);
    }

    @Override
    public SourceSpan getPos() {
        return opPos;
    }

    public interface Operator {
        Value apply(Value first, Value second, SourceSpan pos);

        Operator PLUS = (first, second, pos) -> {
            if (first instanceof Value.NumberValue number1
                && second instanceof Value.NumberValue number2) {
                return new Value.NumberValue(number1.value() + number2.value());
            }
            if (first instanceof Value.StringValue string1
                && second instanceof Value.StringValue string2) {
                return new Value.StringValue(string1.value() + string2.value());
            }
            if (first instanceof Value.ArrayValue array1
                && second instanceof Value.ArrayValue array2) {
                var array = new JsonArray();
                array.addAll(array1.value());
                array.addAll(array2.value());
                return new Value.ArrayValue(array);
            }
            if (first instanceof Value.ObjectValue object1
                && second instanceof Value.ObjectValue object2) {
                var object = new JsonObject();
                object.asMap().putAll(object1.value().asMap());
                object.asMap().putAll(object2.value().asMap());
                return new Value.ObjectValue(object);
            }
            throw new EvaluationException("Can't add %s and %s together".formatted(first, second), pos);
        };
        Operator MINUS = (first, second, pos) -> {
            if (first instanceof Value.NumberValue number1
                && second instanceof Value.NumberValue number2) {
                return new Value.NumberValue(number1.value() - number2.value());
            }
            throw new EvaluationException("Can't subtract %s from %s".formatted(second, first), pos);
        };
        Operator MULTIPLY = (first, second, pos) -> {
            if (!(second instanceof Value.NumberValue number2)) {
                throw new EvaluationException("Can't multiply by %s".formatted(second), pos);
            }
            if (first instanceof Value.NumberValue number1) {
                return new Value.NumberValue(number1.value() * number2.value());
            }
            if (first instanceof Value.StringValue string1) {
                return new Value.StringValue(string1.value().repeat((int) number2.value()));
            }
            if (first instanceof Value.ArrayValue array1) {
                var array = new JsonArray();
                for (int i = 0; i < (int) number2.value(); i++) {
                    array.addAll(array1.value());
                }
                return new Value.ArrayValue(array);
            }
            throw new EvaluationException("Can't multiply %s with %s".formatted(first, second), pos);
        };
        Operator DIVIDE = (first, second, pos) -> {
            if (first instanceof Value.NumberValue number1
                && second instanceof Value.NumberValue number2) {
                return new Value.NumberValue(number1.value() / number2.value());
            }
            throw new EvaluationException("Can't divide %s by %s".formatted(first, second), pos);
        };
        Operator MODULO = (first, second, pos) -> {
            if (first instanceof Value.NumberValue number1
                && second instanceof Value.NumberValue number2) {
                return new Value.NumberValue(number1.value() % number2.value());
            }
            throw new EvaluationException("Can't take %s modulo %s".formatted(first, second), pos);
        };

        Operator AND = (first, second, pos) -> {
            if (first instanceof Value.NumberValue number1
                && second instanceof Value.NumberValue number2) {
                return new Value.NumberValue((int) number1.value() & (int) number2.value());
            }
            if (first instanceof Value.BooleanValue boolean1
                && second instanceof Value.BooleanValue boolean2) {
                return Value.BooleanValue.of(boolean1.value() && boolean2.value());
            }
            throw new EvaluationException("Can't apply and to %s and %s".formatted(first, second), pos);
        };
        Operator OR = (first, second, pos) -> {
            if (first instanceof Value.NumberValue number1
                && second instanceof Value.NumberValue number2) {
                return new Value.NumberValue((int) number1.value() | (int) number2.value());
            }
            if (first instanceof Value.BooleanValue boolean1
                && second instanceof Value.BooleanValue boolean2) {
                return Value.BooleanValue.of(boolean1.value() || boolean2.value());
            }
            throw new EvaluationException("Can't apply or to %s and %s".formatted(first, second), pos);
        };
        Operator XOR = (first, second, pos) -> {
            if (first instanceof Value.NumberValue number1
                && second instanceof Value.NumberValue number2) {
                return new Value.NumberValue((int) number1.value() ^ (int) number2.value());
            }
            if (first instanceof Value.BooleanValue boolean1
                && second instanceof Value.BooleanValue boolean2) {
                return Value.BooleanValue.of(boolean1.value() ^ boolean2.value());
            }
            throw new EvaluationException("Can't apply xor to %s and %s".formatted(first, second), pos);
        };

        Operator EQUALS = (first, second, pos) -> Value.BooleanValue.of(isEqual(first, second));
        Operator NOT_EQUALS = (first, second, pos) -> Value.BooleanValue.of(!isEqual(first, second));
        Operator LESS_THAN = numberComparison((first, second) -> first < second);
        Operator GREATER_THAN = numberComparison((first, second) -> first > second);
        Operator LESS_THAN_EQUAL = numberComparison((first, second) -> first <= second);
        Operator GREATER_THAN_EQUAL = numberComparison((first, second) -> first >= second);

        /**
         * Special operator used for the normal assignment. Returns the second value
         */
        Operator ASSIGN = (first, second, pos) -> second;

        private static boolean isEqual(Value first, Value second) {
            if (first instanceof Value.NumberValue number1
                && second instanceof Value.NumberValue number2) {
                return number1.value() == number2.value();
            }
            if (first instanceof Value.StringValue string1
                && second instanceof Value.StringValue string2) {
                return string1.value().equals(string2.value());
            }
            if (first instanceof Value.ArrayValue array1
                && second instanceof Value.ArrayValue array2) {
                return array1.value().equals(array2.value());
            }
            if (first instanceof Value.ObjectValue object1
                && second instanceof Value.ObjectValue object2) {
                return object1.value().equals(object2.value());
            }
            return false;
        }

        private static Operator numberComparison(BiPredicate<Double, Double> predicate) {
            return (first, second, pos) -> {
                if (first instanceof Value.NumberValue number1
                    && second instanceof Value.NumberValue number2) {
                    return Value.BooleanValue.of(predicate.test(number1.value(), number2.value()));
                }
                throw new EvaluationException("Can't compare %s and %s".formatted(second, first), pos);
            };
        }
    }
}
