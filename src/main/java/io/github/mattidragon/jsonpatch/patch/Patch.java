package io.github.mattidragon.jsonpatch.patch;

import io.github.mattidragon.jsonpatch.lang.runtime.Program;
import net.minecraft.util.Identifier;

public record Patch(Program program, Identifier id, PatchTarget target) {
}
