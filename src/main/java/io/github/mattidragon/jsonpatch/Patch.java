package io.github.mattidragon.jsonpatch;

import io.github.mattidragon.jsonpatch.lang.ast.Program;
import net.minecraft.util.Identifier;

public record Patch(Program program, Identifier target) {
}
