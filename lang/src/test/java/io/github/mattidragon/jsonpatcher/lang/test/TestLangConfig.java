package io.github.mattidragon.jsonpatcher.lang.test;

import io.github.mattidragon.jsonpatcher.lang.LangConfig;

public class TestLangConfig implements LangConfig {
    @Override
    public boolean useJavaStacktrace() {
        return false;
    }

    @Override
    public boolean useShortStacktrace() {
        return false;
    }
}
