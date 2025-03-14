package dev.mattidragon.jsonpatcher.patch.global;

import dev.mattidragon.jsonpatcher.trust.TrustLevel;

import java.nio.file.Path;

public record GlobalPatchSource(String idPrefix, Path path, TrustLevel trustLevel) {
}
