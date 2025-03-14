package dev.mattidragon.jsonpatcher.metapatch;

import dev.mattidragon.jsonpatcher.patch.PatchTarget;

public record FileFilter(PatchTarget target, boolean allow) {
}
