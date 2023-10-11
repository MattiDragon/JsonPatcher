package io.github.mattidragon.jsonpatcher.lang.runtime.function;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.Context;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.statement.Statement;

import java.util.List;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;

public sealed interface PatchFunction {
    Value execute(Context context, List<Value> args, SourceSpan callPos);

    @FunctionalInterface
    non-sealed interface BuiltInPatchFunction extends PatchFunction {
        static BuiltInPatchFunction numberUnary(DoubleUnaryOperator operator) {
            return ((BuiltInPatchFunction) (context, args, callPos) -> {
                if (!(args.get(0) instanceof Value.NumberValue value)) {
                    throw new EvaluationException("Expected argument to be number, was %s".formatted(args.get(0)), callPos);
                }
                return new Value.NumberValue(operator.applyAsDouble(value.value()));
            }).argCount(1);
        }

        default BuiltInPatchFunction argCount(int count) {
            return (context, args, callPos) -> {
                if (args.size() != count) {
                    throw new EvaluationException("Incorrect function argument count: expected %s but found %s".formatted(count, args.size()), callPos);
                }
                return execute(context, args, callPos);
            };
        }
    }

    record DefinedPatchFunction(Statement body, List<Optional<String>> args, Context context) implements PatchFunction {
        public DefinedPatchFunction {
            args = List.copyOf(args);
        }

        @Override
        public Value execute(Context context, List<Value> args, SourceSpan callPos) {
            if (this.args.size() != args.size()) {
                throw new EvaluationException("Incorrect function argument count: expected %s but found %s".formatted(this.args.size(), args.size()), callPos);
            }

            // We use the context the function was created in, not the one it was called in.
            // This allows for closures if we ever allow a function to escape its original scope
            var functionContext = this.context.newScope();

            var rootIndex = this.args.indexOf(Optional.<String>empty());
            if (rootIndex != -1) {
                if (args.get(rootIndex) instanceof Value.ObjectValue root) {
                    functionContext = functionContext.withRoot(root);
                } else {
                    throw new EvaluationException("Only objects can be used in apply statements, tried to use %s".formatted(args.get(rootIndex)), callPos);
                }
            }

            var variables = functionContext.variables();
            for (int i = 0; i < args.size(); i++) {
                var argName = this.args.get(i);
                var argVal = args.get(i);
                argName.ifPresent(s -> variables.createVariableWithShadowing(s, argVal, false));
            }

            try {
                body.run(functionContext);
            } catch (ReturnException r) {
                return r.value;
            } catch (EvaluationException e) {
                throw new EvaluationException("Error while executing function", callPos, e);
            }

            return Value.NullValue.NULL;
        }
    }
}
