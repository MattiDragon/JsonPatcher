package io.github.mattidragon.jsonpatcher.patch;

import io.github.mattidragon.jsonpatcher.lang.runtime.Program;
import net.minecraft.util.Identifier;

import java.util.List;

public record Patch(Program program, Identifier id, List<PatchTarget> target, boolean isMeta) {
}
