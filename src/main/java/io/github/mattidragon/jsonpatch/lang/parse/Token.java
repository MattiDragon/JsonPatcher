package io.github.mattidragon.jsonpatch.lang.parse;

public sealed interface Token {
    record NumberToken(double value) implements Token {
    }

    record StringToken(String value) implements Token {
    }

    record WordToken(String value) implements Token {
    }

    enum SimpleToken implements Token {
        EQUALS,
        DOT,
        AT_SIGN,
        BEGIN_CURLY,
        END_CURLY,
        BEGIN_PAREN,
        END_PAREN,
        BEGIN_SQUARE,
        END_SQUARE,
        SEMICOLON,
        COLON,
        COMMA,
        BANG,
        QUESTION_MARK,
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
        THIS
    }
}
