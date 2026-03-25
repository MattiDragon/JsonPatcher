package dev.mattidragon.jsonpatcher.patch;

import dev.mattidragon.jsonpatcher.lang.runtime.environment.EvaluationEnvironment;
import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import net.minecraft.resources.Identifier;

import java.util.List;

public record Patch(
        EvaluationEnvironment.AddedProgram program,
        Identifier id,
        List<PatchTarget> target,
        double priority,
        boolean isMeta,
        TrustLevel trustLevel
) implements LoadedProgram {
    @Override
    public String name() {
        return id.toString();
    }
}
