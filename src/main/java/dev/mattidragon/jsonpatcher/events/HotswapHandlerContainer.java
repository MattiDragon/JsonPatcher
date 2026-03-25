package dev.mattidragon.jsonpatcher.events;

import dev.mattidragon.jsonpatcher.lang.runtime.EvaluationContext;
import dev.mattidragon.jsonpatcher.lang.runtime.lib.reflection.LambdaBridgeGenerator;
import dev.mattidragon.jsonpatcher.lang.runtime.value.PatchFunction;
import dev.mattidragon.jsonpatcher.lang.runtime.value.Value;
import net.fabricmc.fabric.api.event.Event;

import java.util.HashMap;
import java.util.Map;

public class HotswapHandlerContainer implements HandlerContainer {
    private final Map<Event<?>, HandlerCell> handlers = new HashMap<>();

    @Override
    public void registerHandler(Event<?> event, Value.FunctionValue handler, Class<?> handlerType, EvaluationContext context) {
        var cell = handlers.computeIfAbsent(event, _ -> {
            var handlerCell = new HandlerCell(handler);
            var wrappedFunction = new Value.FunctionValue((PatchFunction.BuiltInPatchFunction) (ctx, args) -> ctx.execute(handlerCell.handler.function(), args));
            var convertedHandler = LambdaBridgeGenerator.createLambdaBridge(handlerType, context, wrappedFunction);
            register(event, convertedHandler);
            return handlerCell;
        });

        cell.handler = handler;
    }

    private static class HandlerCell {
        private Value.FunctionValue handler;

        public HandlerCell(Value.FunctionValue handler) {
            this.handler = handler;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void register(Event<?> event, Object handler) {
        ((Event<T>) event).register((T) handler);
    }
}
