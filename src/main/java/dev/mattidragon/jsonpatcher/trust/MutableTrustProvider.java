package dev.mattidragon.jsonpatcher.trust;

public interface MutableTrustProvider extends TrustProvider {
    void jsonpatcher$setTrustLevel(TrustLevel trustLevel);
}
