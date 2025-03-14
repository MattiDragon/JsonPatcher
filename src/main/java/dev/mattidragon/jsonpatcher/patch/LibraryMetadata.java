package dev.mattidragon.jsonpatcher.patch;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record LibraryMetadata(boolean shared) {
    public static final LibraryMetadata DEFAULT = new LibraryMetadata(false);
    public static final Codec<LibraryMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("shared", DEFAULT.shared).forGetter(LibraryMetadata::shared)
    ).apply(instance, LibraryMetadata::new));
}
