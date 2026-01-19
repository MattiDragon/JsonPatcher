package dev.mattidragon.jsonpatcher.events;

import dev.mattidragon.jsonpatcher.config.Config;
import dev.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import dev.mattidragon.jsonpatcher.lang.runtime.value.PatchFunction;
import dev.mattidragon.jsonpatcher.lang.runtime.value.Value;
import net.fabricmc.fabric.api.event.Event;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class EventsLibrary {
    private static final Map<String, HandlerContainer> CONTAINERS = new HashMap<>();

    public Value container(Value.StringValue name) {
        var container = CONTAINERS.computeIfAbsent(name.value(), s -> Config.MANAGER.get().hotswapEvents() ? new HotswapHandlerContainer() : new SimpleHandlerContainer());

        return new Value.ObjectValue(Map.of(
                "registerFabric", new Value.FunctionValue((PatchFunction.BuiltInPatchFunction) (ctx, args) -> {
                    if (args.size() != 3) {
                        throw new IllegalArgumentException("Expected exactly 3 arguments when registering to fabric event");
                    }
                    if (!(args.get(0) instanceof Value.StringValue(var className))) {
                        throw new IllegalArgumentException("Expected first argument to be a string representing the event class name");
                    }
                    if (!(args.get(1) instanceof Value.StringValue(var fieldName))) {
                        throw new IllegalArgumentException("Expected second argument to be a string representing the event field name");
                    }
                    if (!(args.get(2) instanceof Value.FunctionValue handler)) {
                        throw new IllegalArgumentException("Expected third argument to be a function representing the event handler");
                    }

                    try {
                        var clazz = Class.forName(className);
                        var field = clazz.getDeclaredField(fieldName);

                        registerFabric(container, field, handler, ctx);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Could not find event class", e);
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException("Could not find event field", e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Could not access event field", e);
                    }

                    return Value.NullValue.NULL;
                })
        ), true);
    }

    private void registerFabric(HandlerContainer container, Field field, Value.FunctionValue handler, EvaluationContext context) throws IllegalAccessException {
        // Maybe handle subclasses later
        if (field.getType() != Event.class) {
            throw new IllegalArgumentException("Field is not a fabric event");
        }

        // Extract actual event type from generic field signature
        var handlerType = switch (field.getGenericType()) {
            case ParameterizedType type -> {
                var typeArgs = type.getActualTypeArguments();
                if (typeArgs.length != 1) {
                    throw new IllegalArgumentException("Unexpected number of type arguments for fabric event: " + typeArgs.length);
                }
                yield typeArgs[0];
            }
            case Class<?> type -> throw new IllegalArgumentException("Event field is a raw type: " + type);
            default -> throw new IllegalStateException("Unexpected field type: " + field.getGenericType());
        };
        var rawHandlerType = switch (handlerType) {
            case ParameterizedType type -> (Class<?>) type.getRawType();
            case Class<?> type -> type;
            default -> throw new IllegalStateException("Unexpected handler type: " + handlerType);
        };

        var event = (Event<?>) field.get(null);
        container.registerHandler(event, handler, rawHandlerType, context);
    }
}
