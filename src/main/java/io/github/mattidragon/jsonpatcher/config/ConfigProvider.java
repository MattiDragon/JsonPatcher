package io.github.mattidragon.jsonpatcher.config;

import io.github.mattidragon.jsonpatcher.lang.LangConfig;

public class ConfigProvider implements LangConfig {
    @Override
    public boolean useJavaStacktrace() {
        return Config.MANAGER.get().useJavaStacktrace();
    }

    @Override
    public boolean useShortStacktrace() {
        return Config.MANAGER.get().useShortStacktrace();
    }
}
