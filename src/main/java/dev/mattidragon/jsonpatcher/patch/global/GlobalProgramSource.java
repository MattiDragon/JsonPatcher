package dev.mattidragon.jsonpatcher.patch.global;

import dev.mattidragon.jsonpatcher.trust.TrustLevel;

import java.nio.file.Path;

public record GlobalProgramSource(String idPrefix, Path path, TrustLevel trustLevel) {
}
