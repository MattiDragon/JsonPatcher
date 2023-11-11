package io.github.mattidragon.jsonpatcher.lang.runtime.stdlib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation allows you to override the name of a function in the library.
 * This is useful if you want to have a function with a name that is not a valid Java identifier.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FunctionName {
    String value();
}
