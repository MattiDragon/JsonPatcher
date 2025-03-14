package io.github.mattidragon.jsonpatcher.patch;

import dev.mattidragon.jsonpatcher.lang.runtime.environment.EvaluationEnvironment;

public interface BasePatch {
    EvaluationEnvironment.AddedProgram program();
    String name();
}
