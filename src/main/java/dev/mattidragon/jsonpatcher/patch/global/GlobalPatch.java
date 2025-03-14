package dev.mattidragon.jsonpatcher.patch.global;

import dev.mattidragon.jsonpatcher.lang.runtime.environment.EvaluationEnvironment;
import dev.mattidragon.jsonpatcher.patch.BasePatch;
import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import org.jetbrains.annotations.Nullable;

public record GlobalPatch(
        EvaluationEnvironment.AddedProgram program,
        String id,
        double priority,
        TrustLevel trustLevel,
        @Nullable Entrypoint entrypoint
        ) implements BasePatch {

    @Override
    public String name() {
        return id;
    }

    public enum Entrypoint {
        MAIN, CLIENT
    }
}
