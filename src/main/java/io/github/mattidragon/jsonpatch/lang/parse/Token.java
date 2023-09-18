package io.github.mattidragon.jsonpatch.lang.parse;

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

        MINUS,
        PLUS,
        STAR,
        SLASH,
        PERCENT,
        TILDE
    }

    enum KeywordToken implements Token {
        TRUE,
        FALSE,
        NULL,
        APPLY,
        THIS,
        IF,
        ELSE,
        FOR,
        IN,
        VAR,
        VAL,
        DELETE,
        FUNCTION
    }
}
