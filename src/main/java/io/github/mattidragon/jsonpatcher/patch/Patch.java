package io.github.mattidragon.jsonpatcher.patch;

import io.github.mattidragon.jsonpatcher.lang.runtime.Program;
import net.minecraft.util.Identifier;

import java.util.Optional;

public record Patch(Program program, Identifier id, Optional<PatchTarget> target) {
}
