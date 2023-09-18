package io.github.mattidragon.jsonpatch.lang.runtime;

import com.google.gson.JsonElement;
import io.github.mattidragon.jsonpatch.lang.runtime.function.BuiltInFunctions;
import net.minecraft.util.JsonHelper;

public record Context(Value.ObjectValue root, VariableStack variables) {
    public Context withRoot(Value.ObjectValue root) {
        return new Context(root, variables);
    }

    public Context newScope() {
        return new Context(root, new VariableStack(variables));
    }

    public static Context create(JsonElement json) {
        return new Context(new Value.ObjectValue(JsonHelper.asObject(json, "patched file")),
                new VariableStack(BuiltInFunctions.ALL_FUNCTIONS));
    }
}
