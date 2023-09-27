package io.github.mattidragon.jsonpatcher.lang.parse.pratt;

/**
 * This enum contains precedence values for the pratt parser.
 * The ordinal of the enum constants are used for comparison,
 * meaning that it's simple to add new levels in between without
 * risking breaks.
 */
public enum Precedence {
    /**
     * Base precedence level. Used at the start of expressions.
     */
    ROOT,
    ASSIGNMENT,
    OR,
    AND,
    BITWISE_OR,
    BITWISE_XOR,
    BITWISE_AND,
    EQUALITY,
    COMPARISON,
    BIT_SHIFT,
    SUM,
    PRODUCT,
    EXPONENT,
    PREFIX,
    POSTFIX
}
