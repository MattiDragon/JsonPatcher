package io.github.mattidragon.jsonpatcher;

import io.github.mattidragon.jsonpatcher.lang.parse.PatchMetadata;
import io.github.mattidragon.jsonpatcher.patch.PatchTarget;

public record TargetPatchMetadata(PatchTarget target) {
    public static final PatchMetadata.Key<TargetPatchMetadata> KEY = new PatchMetadata.Key<>("target");
    public static final PatchMetadata.MetaParser.SingleValueMetaParser<TargetPatchMetadata> PARSER = parser -> new TargetPatchMetadata(PatchTarget.parse(parser));
}
