package dev.mattidragon.jsonpatcher.trust;

public interface TrustProvider {
    ScopedValue<TrustLevel> TRUST_LEVEL_PASSTHROUGH = ScopedValue.newInstance();

    TrustLevel jsonpatcher$trustLevel();
}
