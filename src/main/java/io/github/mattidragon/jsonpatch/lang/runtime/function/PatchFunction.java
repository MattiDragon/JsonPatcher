package io.github.mattidragon.jsonpatch.lang.runtime.function;

import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatch.lang.runtime.Context;
import io.github.mattidragon.jsonpatch.lang.runtime.EvaluationException;
import io.github.mattidragon.jsonpatch.lang.runtime.Value;
import io.github.mattidragon.jsonpatch.lang.runtime.statement.BlockStatement;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

public sealed interface PatchFunction {
    Value execute(Context context, List<Value> args, SourceSpan callPos);

    @FunctionalInterface
    non-sealed interface BuiltInPatchFunction extends PatchFunction {
        static BuiltInPatchFunction numberUnary(DoubleUnaryOperator operator) {
            return (context, args, callPos) -> {
                if (args.size() != 1) {
                    throw new EvaluationException("Incorrect function argument count: expected 1 but found %s".formatted(args.size()), callPos);
                }
                if (!(args.get(0) instanceof Value.NumberValue value)) {
                    throw new EvaluationException("Expected argument to be number, was %s".formatted(args.get(0)), callPos);
                }
                return new Value.NumberValue(operator.applyAsDouble(value.value()));
            };
        }
    }

    record DefinedPatchFunction(BlockStatement body, List<String> args) implements PatchFunction {
        public DefinedPatchFunction {
            args = List.copyOf(args);
        }

        @Override
        public Value execute(Context context, List<Value> args, SourceSpan callPos) {
            if (this.args.size() != args.size()) {
                throw new EvaluationException("Incorrect function argument count: expected %s but found %s".formatted(this.args.size(), args.size()), callPos);
            }

            var functionContext = context.newScope();
            for (int i = 0; i < args.size(); i++) {
                functionContext.variables().createVariableWithShadowing(this.args.get(i), args.get(i), false);
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
