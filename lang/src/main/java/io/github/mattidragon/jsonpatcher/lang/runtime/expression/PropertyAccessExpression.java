package io.github.mattidragon.jsonpatcher.lang.runtime.expression;

import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import io.github.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import io.github.mattidragon.jsonpatcher.lang.runtime.Value;
import io.github.mattidragon.jsonpatcher.lang.runtime.stdlib.Libraries;

public record PropertyAccessExpression(Expression parent, String name, SourceSpan pos) implements Reference {
    @Override
    public Value get(EvaluationContext context) {
        var parent = this.parent.evaluate(context);

        if (parent instanceof Value.ObjectValue objectValue) {
            return objectValue.get(name, pos);
        } else if (parent instanceof Value.ArrayValue arrayValue) {
            if (name.equals("length")) {
                return new Value.NumberValue(arrayValue.value().size());
            } else if (Libraries.ArraysLibrary.METHODS.containsKey(name)) {
                var function = Libraries.ArraysLibrary.METHODS.get(name);
                return new Value.FunctionValue(function.bind(arrayValue));
            } else {
                throw error("Tried to read invalid property %s of %s.".formatted(name, parent));
            }
        } else if (parent instanceof Value.StringValue stringValue) {
            if (Libraries.StringsLibrary.METHODS.containsKey(name)) {
                var function = Libraries.StringsLibrary.METHODS.get(name);
                return new Value.FunctionValue(function.bind(stringValue));
            } else {
                throw error("Tried to read invalid property %s of %s.".formatted(name, parent));
            }
        } else if (parent instanceof Value.FunctionValue functionValue) {
            if (Libraries.FunctionsLibrary.METHODS.containsKey(name)) {
                var function = Libraries.FunctionsLibrary.METHODS.get(name);
                return new Value.FunctionValue(function.bind(functionValue));
            } else {
                throw error("Tried to read invalid property %s of %s.".formatted(name, parent));
            }

        } else {
            throw error("Tried to read property %s of %s. Only objects and arrays have properties.".formatted(name, parent));
        }
    }

    @Override
    public void set(EvaluationContext context, Value value) {
        var parent = this.parent.evaluate(context);
        if (parent instanceof Value.ObjectValue objectValue) {
            objectValue.set(name, value, pos);
        } else {
            throw error("Tried to write property %s of %s. Only objects have properties.".formatted(name, parent));
        }
    }

    @Override
    public void delete(EvaluationContext context) {
        var parent = this.parent.evaluate(context);
        if (parent instanceof Value.ObjectValue objectValue) {
            objectValue.remove(name, pos);
        } else {
            throw error("Tried to delete property %s of %s. Only objects have properties.".formatted(name, parent));
        }
    }

    @Override
    public SourceSpan getPos() {
        return pos;
    }
}
