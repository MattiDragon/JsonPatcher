package io.github.mattidragon.jsonpatcher.patch;

import io.github.mattidragon.jsonpatcher.lang.LangConfig;

public class PatchingException extends RuntimeException {
    public PatchingException(String message) {
        super(message);
    }

    public PatchingException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        if (LangConfig.INSTANCE.useJavaStacktrace()) return super.fillInStackTrace();
        return this;
    }

    @Override
    public synchronized Throwable getCause() {
        if (LangConfig.INSTANCE.useJavaStacktrace()) return super.getCause();

        return null;
    }
}
