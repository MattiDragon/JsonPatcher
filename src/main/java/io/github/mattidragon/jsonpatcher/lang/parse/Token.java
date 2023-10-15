package io.github.mattidragon.jsonpatcher.lang.parse;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public sealed interface Token {
    record NumberToken(double value) implements Token {
    }

    record StringToken(String value) implements Token {
    }

    record WordToken(String value) implements Token {
    }

    enum SimpleToken implements Token {
        ASSIGN,

        NOT_EQUALS,
        EQUALS,
        LESS_THAN,
        GREATER_THAN,
        LESS_THAN_EQUAL,
        GREATER_THAN_EQUAL,

        STAR_ASSIGN,
        PLUS_ASSIGN,
        MINUS_ASSIGN,
        SLASH_ASSIGN,
        PERCENT_ASSIGN,
        AND_ASSIGN,
        OR_ASSIGN,
        XOR_ASSIGN,

        AND,
        OR,
        XOR,

        DOUBLE_AND,
        DOUBLE_OR,
        DOUBLE_MINUS,
        DOUBLE_PLUS,
        DOUBLE_STAR,
        DOUBLE_BANG,

        BEGIN_CURLY,
        END_CURLY,
        BEGIN_PAREN,
        END_PAREN,
        BEGIN_SQUARE,
        END_SQUARE,

        DOT,
        COMMA,
        COLON,
        SEMICOLON,

        BANG,
        QUESTION_MARK,
        DOLLAR,
        AT_SIGN,

        MINUS,
        PLUS,
        STAR,
        SLASH,
        PERCENT,
        TILDE
    }

    enum KeywordToken implements Token {
        TRUE("true"),
        FALSE("false"),
        NULL("null"),
        APPLY("apply"),
        THIS("this"),
        IF("if"),
        ELSE("else"),
        IN("in"),
        VAR("var"),
        VAL("val"),
        DELETE("delete"),
        FUNCTION("function"),
        RETURN("return"),
        IMPORT("import"),
        WHILE("while"),
        FOR("for"),
        FOREACH("foreach");

        public static final Map<String, KeywordToken> ALL = Arrays.stream(KeywordToken.values()).collect(Collectors.toUnmodifiableMap(KeywordToken::getValue, Function.identity()));

        private final String value;

        KeywordToken(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
