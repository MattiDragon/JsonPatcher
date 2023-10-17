package io.github.mattidragon.jsonpatcher.patch;

import io.github.mattidragon.jsonpatcher.lang.runtime.Program;
import net.minecraft.util.Identifier;

public record Patch(Program program, Identifier id, java.util.List<PatchTarget> target) {
}
