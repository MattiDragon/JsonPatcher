package io.github.mattidragon.jsonpatcher.trust;

import net.minecraft.resource.ResourcePack;

public class TrustChecker {
    public static TrustLevel getTrust(ResourcePack pack) {
        return ((TrustProvidingPack) pack).jsonpatcher$trustLevel();
    }
}
