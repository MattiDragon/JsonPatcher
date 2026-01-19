package dev.mattidragon.jsonpatcher.events;

import dev.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import dev.mattidragon.jsonpatcher.lang.runtime.value.Value;
import net.fabricmc.fabric.api.event.Event;

public interface HandlerContainer {
    void registerHandler(Event<?> event, Value.FunctionValue handler, Class<?> handlerType, EvaluationContext context);
}
