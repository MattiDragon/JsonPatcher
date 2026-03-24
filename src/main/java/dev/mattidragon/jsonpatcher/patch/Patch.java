package dev.mattidragon.jsonpatcher.patch;

import dev.mattidragon.jsonpatcher.lang.runtime.environment.EvaluationEnvironment;
import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record Patch(
        EvaluationEnvironment.AddedProgram program,
        ResourceLocation id,
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
