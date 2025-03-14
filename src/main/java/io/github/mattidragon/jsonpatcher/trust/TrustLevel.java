package io.github.mattidragon.jsonpatcher.trust;

public enum TrustLevel {
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
     * Resources that are known to originate in mods.
     * These can always be trusted, as a mod can do arbitrary code execution without us.
     */
    MOD
}
