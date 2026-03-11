package dev.mattidragon.jsonpatcher.patch;

import dev.mattidragon.jsonpatcher.lang.runtime.environment.EvaluationEnvironment;

public interface LoadedProgram {
    EvaluationEnvironment.AddedProgram program();
    String name();
}
