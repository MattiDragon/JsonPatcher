package dev.mattidragon.jsonpatcher.trust;

import net.minecraft.resource.ResourcePack;

public class TrustChecker {
    public static TrustLevel getTrust(ResourcePack pack) {
        return ((TrustProvider) pack).jsonpatcher$trustLevel();
    }
}
