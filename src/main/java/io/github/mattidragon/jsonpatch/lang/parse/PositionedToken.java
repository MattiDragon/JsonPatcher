package io.github.mattidragon.jsonpatch.lang.parse;

public sealed abstract class PositionedToken<T extends Token> {
    private final SourcePos from;
    private final SourcePos to;
    private final T token;

    protected PositionedToken(SourcePos from, SourcePos to, T token) {
        this.from = from;
        this.to = to;
        this.token = token;
    }

    public SourcePos getFrom() {
        return from;
    }

    public SourcePos getTo() {
        return to;
    }

    public T getToken() {
        return token;
    }

    public static PositionedToken<?> of(SourcePos from, SourcePos to, Token token) {
        if (token instanceof Token.StringToken stringToken) return new StringToken(from, to, stringToken);
        if (token instanceof Token.NumberToken numberToken) return new NumberToken(from, to, numberToken);
        if (token instanceof Token.WordToken wordToken) return new WordToken(from, to, wordToken);
        if (token instanceof Token.KeywordToken keywordToken) return new KeywordToken(from, to, keywordToken);
        if (token instanceof Token.SimpleToken simpleToken) return new SimpleToken(from, to, simpleToken);
        throw new IllegalStateException("Unsupported token type");
    }

    public SourceSpan getPos() {
        return new SourceSpan(from, to);
    }

    public static final class SimpleToken extends PositionedToken<Token.SimpleToken> {
        public SimpleToken(SourcePos from, SourcePos to, Token.SimpleToken token) {
            super(from, to, token);
        }
    }

    public static final class KeywordToken extends PositionedToken<Token.KeywordToken> {
        public KeywordToken(SourcePos from, SourcePos to, Token.KeywordToken token) {
            super(from, to, token);
        }
    }

    public static final class WordToken extends PositionedToken<Token.WordToken> {
        public WordToken(SourcePos from, SourcePos to, Token.WordToken token) {
            super(from, to, token);
        }
    }

    public static final class StringToken extends PositionedToken<Token.StringToken> {
        public StringToken(SourcePos from, SourcePos to, Token.StringToken token) {
            super(from, to, token);
        }
    }

    public static final class NumberToken extends PositionedToken<Token.NumberToken> {
        public NumberToken(SourcePos from, SourcePos to, Token.NumberToken token) {
            super(from, to, token);
        }
    }
}
