package io.github.mattidragon.jsonpatcher.patch.global;

import dev.mattidragon.jsonpatcher.lang.runtime.environment.EvaluationEnvironment;
import io.github.mattidragon.jsonpatcher.patch.BasePatch;
import io.github.mattidragon.jsonpatcher.trust.TrustLevel;
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
