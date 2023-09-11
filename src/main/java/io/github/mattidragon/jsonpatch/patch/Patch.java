package io.github.mattidragon.jsonpatch.patch;

import io.github.mattidragon.jsonpatch.lang.runtime.Program;

public record Patch(Program program, PatchTarget target) {
}
