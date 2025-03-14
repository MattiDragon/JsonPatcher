package io.github.mattidragon.jsonpatcher.patch.global;

import io.github.mattidragon.jsonpatcher.trust.TrustLevel;

import java.nio.file.Path;

public record GlobalPatchSource(String idPrefix, Path path, TrustLevel trustLevel) {
}
