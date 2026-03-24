package dev.mattidragon.jsonpatcher.trust;

import net.minecraft.server.packs.PackResources;

public class TrustChecker {
    public static TrustLevel getTrust(PackResources pack) {
        return ((TrustProvider) pack).jsonpatcher$trustLevel();
    }
}
