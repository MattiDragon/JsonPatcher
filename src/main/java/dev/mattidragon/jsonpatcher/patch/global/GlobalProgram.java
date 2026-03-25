package dev.mattidragon.jsonpatcher.patch.global;

import dev.mattidragon.jsonpatcher.lang.runtime.environment.EvaluationEnvironment;
import dev.mattidragon.jsonpatcher.patch.LoadedProgram;
import dev.mattidragon.jsonpatcher.trust.TrustLevel;
import org.jspecify.annotations.Nullable;

public record GlobalProgram(
        EvaluationEnvironment.AddedProgram program,
        String id,
        double priority,
        TrustLevel trustLevel,
        @Nullable Entrypoint entrypoint
        ) implements LoadedProgram {

    @Override
    public String name() {
        return id;
    }

    public enum Entrypoint {
        MAIN, CLIENT
    }
}
