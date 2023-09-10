package io.github.mattidragon.jsonpatch.lang.parse;

public sealed abstract class PositionedToken<T extends Token> {
    private final SourceSpan pos;
    private final T token;

    protected PositionedToken(SourceSpan pos, T token) {
        this.pos = pos;
        this.token = token;
    }

    public SourcePos getFrom() {
        return pos.from();
    }

    public SourcePos getTo() {
        return pos.to();
    }

    public T getToken() {
        return token;
    }

    public SourceSpan getPos() {
        return pos;
    }

    public static PositionedToken<?> of(SourceSpan pos, Token token) {
        if (token instanceof Token.StringToken stringToken) return new StringToken(pos, stringToken);
        if (token instanceof Token.NumberToken numberToken) return new NumberToken(pos, numberToken);
        if (token instanceof Token.WordToken wordToken) return new WordToken(pos, wordToken);
        if (token instanceof Token.KeywordToken keywordToken) return new KeywordToken(pos, keywordToken);
        if (token instanceof Token.SimpleToken simpleToken) return new SimpleToken(pos, simpleToken);
        throw new IllegalStateException("Unsupported token type");
    }

    public static final class SimpleToken extends PositionedToken<Token.SimpleToken> {
        private SimpleToken(SourceSpan pos, Token.SimpleToken token) {
            super(pos, token);
        }
    }

    public static final class KeywordToken extends PositionedToken<Token.KeywordToken> {
        private KeywordToken(SourceSpan pos, Token.KeywordToken token) {
            super(pos, token);
        }
    }

    public static final class WordToken extends PositionedToken<Token.WordToken> {
        private WordToken(SourceSpan pos, Token.WordToken token) {
            super(pos, token);
        }
    }

    public static final class StringToken extends PositionedToken<Token.StringToken> {
        private StringToken(SourceSpan pos, Token.StringToken token) {
            super(pos, token);
        }
    }

    public static final class NumberToken extends PositionedToken<Token.NumberToken> {
        private NumberToken(SourceSpan pos, Token.NumberToken token) {
            super(pos, token);
        }
    }
}
