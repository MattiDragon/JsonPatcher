package dev.mattidragon.jsonpatcher.events;

import dev.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import dev.mattidragon.jsonpatcher.lang.runtime.lib.reflection.LambdaBridgeGenerator;
import dev.mattidragon.jsonpatcher.lang.runtime.value.Value;
import net.fabricmc.fabric.api.event.Event;

import java.util.HashSet;
import java.util.Set;

public class SimpleHandlerContainer implements HandlerContainer {
    private final Set<Event<?>> existing = new HashSet<>();

    @Override
    public void registerHandler(Event<?> event, Value.FunctionValue handler, Class<?> handlerType, EvaluationContext context) {
        // Prefer old handler over duplicates
        if (!existing.add(event)) {
            return;
        }

        var convertedHandler = LambdaBridgeGenerator.createLambdaBridge(handlerType, context, handler);
        register(event, convertedHandler);
    }

    @SuppressWarnings("unchecked")
    private <T> void register(Event<?> event, Object handler) {
        ((Event<T>) event).register((T) handler);
    }
}
