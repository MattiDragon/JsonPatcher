package io.github.mattidragon.jsonpatcher.patch;

import dev.mattidragon.jsonpatcher.lang.runtime.environment.EvaluationEnvironment;
import net.minecraft.util.Identifier;

import java.util.List;

public record Patch(EvaluationEnvironment.AddedProgram program, Identifier id, List<PatchTarget> target, double priority, boolean isMeta) {
}
