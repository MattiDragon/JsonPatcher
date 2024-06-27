package io.github.mattidragon.jsonpatcher.metapatch;

import io.github.mattidragon.jsonpatcher.patch.PatchTarget;

public record FileFilter(PatchTarget target, boolean allow) {
}
