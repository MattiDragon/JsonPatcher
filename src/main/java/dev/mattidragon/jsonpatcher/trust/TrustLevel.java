package dev.mattidragon.jsonpatcher.trust;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

import java.util.Locale;

public enum TrustLevel implements StringIdentifiable {
    /**
     * Any remote resources or otherwise untrusted resources
     */
    UNTRUSTED,
    /**
     * Resources that are known to originate on the users computer.
     * For example standard resource packs or datapacks.
     */
    LOCAL,
    /**
     * Resources that originate from global modpack files.
     * These can generally trusted as modpack dev can add any mods they want.
     */
    MODPACK,
    /**
     * Resources that are known to originate in mods.
     * These can always be trusted, as a mod can do arbitrary code execution without us.
     */
    MOD;

    public static final Codec<TrustLevel> CODEC = StringIdentifiable.createCodec(TrustLevel::values);

    @Override
    public String asString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
