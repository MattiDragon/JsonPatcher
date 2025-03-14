package dev.mattidragon.jsonpatcher.patch;

public class PatchingException extends RuntimeException {
    public PatchingException(String message) {
        super(message);
    }

    public PatchingException(String message, Throwable cause) {
        super(message, cause);
    }
}
