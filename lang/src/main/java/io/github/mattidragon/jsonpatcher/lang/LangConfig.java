package io.github.mattidragon.jsonpatcher.lang;

import java.util.ServiceLoader;

public interface LangConfig {
    LangConfig INSTANCE = ServiceLoader.load(LangConfig.class).findFirst().orElseThrow();

    boolean useJavaStacktrace();
    boolean useShortStacktrace();
}
