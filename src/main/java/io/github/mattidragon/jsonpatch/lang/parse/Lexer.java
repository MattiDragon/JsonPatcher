package io.github.mattidragon.jsonpatch.lang.parse;

import java.util.*;

public class Lexer {
    private static final int TAB_WIDTH = 4;
    private final String program;
    private final String filename;
    private final ArrayList<PositionedToken<?>> tokens = new ArrayList<>();
    private final Map<String, Optional<String>> metadata = new HashMap<>();
    private int current = 0;
    private int currentLine = 1;
    private int currentColumn = 1;

    public Lexer(String program, String filename) {
        this.program = program;
        this.filename = filename;
    }

    public Result lex() {
        while (hasNext()) {
            var c = next();
            switch (c) {
                case '{' -> addParsedToken(Token.SimpleToken.BEGIN_CURLY, 1);
                case '}' -> addParsedToken(Token.SimpleToken.END_CURLY, 1);
                case '[' -> addParsedToken(Token.SimpleToken.BEGIN_SQUARE, 1);
                case ']' -> addParsedToken(Token.SimpleToken.END_SQUARE, 1);
                case '(' -> addParsedToken(Token.SimpleToken.BEGIN_PAREN, 1);
                case ')' -> addParsedToken(Token.SimpleToken.END_PAREN, 1);
                case ';' -> addParsedToken(Token.SimpleToken.SEMICOLON, 1);
                case ':' -> addParsedToken(Token.SimpleToken.COLON, 1);
                case ',' -> addParsedToken(Token.SimpleToken.COMMA, 1);
                case '.' -> addParsedToken(Token.SimpleToken.DOT, 1);
                case '=' -> addParsedToken(Token.SimpleToken.EQUALS, 1);
                case '+' -> addParsedToken(Token.SimpleToken.PLUS, 1);
                case '-' -> addParsedToken(Token.SimpleToken.MINUS, 1);
                case '*' -> addParsedToken(Token.SimpleToken.STAR, 1);
                case '/' -> addParsedToken(Token.SimpleToken.SLASH, 1);
                case '%' -> addParsedToken(Token.SimpleToken.PERCENT, 1);
                case '~' -> addParsedToken(Token.SimpleToken.TILDE, 1);
                case '!' -> addParsedToken(Token.SimpleToken.BANG, 1);
                case '?' -> addParsedToken(Token.SimpleToken.QUESTION_MARK, 1);

                case ' ', '\r', '\n', '\t' -> {}

                case '"', '\'' -> readString(c);

                case '@' -> readMetaLine();
                case '#' -> skipComment();

                default -> {
                    if (c >= '0' && c <= '9') readNumber(c);
                    else if (isWordStartChar(c)) readWord(c);
                    else throw error("Unexpected character: %c (0x%x)".formatted(c, (int) c), 1);
                }
            }
        }

        return new Result(tokens, metadata);
    }

    private void readMetaLine() {
        skipWhitespace();
        var key = new StringBuilder();
        for (var c = peek(); isWordChar(c); c = peek()) {
            key.append(next());
        }
        skipWhitespace();
        if (hasNext() && peek() != '\r' && peek() != '\n') {
            var value = new StringBuilder();
            while (hasNext() && peek() != '\r' && peek() != '\n') {
                value.append(next());
            }
            metadata.put(key.toString(), Optional.of(value.toString().trim()));
        } else {
            metadata.put(key.toString(), Optional.empty());
        }
    }

    private void skipWhitespace() {
        while (hasNext() && peek() == ' ' || peek() == '\t') next();
    }

    private void skipComment() {
        while (hasNext() && peek() != '\n') {
            next();
        }
    }

    private void readNumber(char c) {
        var string = new StringBuilder();
        var beginPos = currentColumn - 1;
        string.append(c);
        for (c = peek(); c >= '0' && c <= '9'; c = peek()) {
            string.append(next());
        }
        if (peek() == '.') string.append(next());
        for (c = peek(); c >= '0' && c <= '9'; c = peek()) {
            string.append(next());
        }

        var token = new Token.NumberToken(Double.parseDouble(string.toString()));
        addParsedToken(token, currentColumn - beginPos);
    }

    private void readWord(char c) {
        var string = new StringBuilder();
        var length = 1;
        string.append(c);
        for (c = peek(); isWordChar(c); c = peek()) {
            string.append(next());
            length++;
        }

        var token = switch (string.toString()) {
            case "true" -> Token.KeywordToken.TRUE;
            case "false" -> Token.KeywordToken.FALSE;
            case "null" -> Token.KeywordToken.NULL;
            case "apply" -> Token.KeywordToken.APPLY;
            case "this" -> Token.KeywordToken.THIS;
            default -> new Token.WordToken(string.toString());
        };
        addParsedToken(token, length);
    }

    private boolean isWordStartChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isWordChar(char c) {
        return isWordStartChar(c) || (c >= '0' && c <= '9') || c == '$';
    }

    private void readString(char begin) {
        var string = new StringBuilder();
        var beginPos = currentColumn - 1;

        for (char c = next(); c != begin; c = next()) {
            switch (c) {
                case '\\' -> {
                    var escaped = next();
                    switch (escaped) {
                        case 'n' -> string.append('\n');
                        case 'r' -> string.append('\r');
                        case 't' -> string.append('\t');
                        case 'b' -> string.append('\b');
                        case '"' -> string.append('"');
                        case '\'' -> string.append('\'');
                        case '\\' -> string.append('\\');
                        default -> throw error("Unknown escape sequence: \\%c".formatted(escaped), 1);
                    }
                }
                case '\n', '\r' -> throw error("Multiline strings aren't supported. Did you forget a quote?");
                default -> string.append(c);
            }
        }

        var token = begin == '"' ? new Token.StringToken(string.toString()) : new Token.WordToken(string.toString());
        addParsedToken(token, currentColumn - beginPos);
    }

    private boolean hasNext() {
        return current < program.length();
    }

    private char peek() {
        return program.charAt(current);
    }

    private char next() {
        if (!hasNext()) throw error("Unexpected end of file");
        var c = program.charAt(current++);
        if (c == '\n') {
            currentLine++;
            currentColumn = 1;
        } if (c == '\t') {
            currentColumn += TAB_WIDTH;
        } else {
            currentColumn++;
        }

        return c;
    }

    private void addParsedToken(Token token, int length) {
        tokens.add(PositionedToken.of(
                new SourcePos(filename, currentLine, currentColumn - length),
                new SourcePos(filename, currentLine, currentLine),
                token));
    }
    
    private LexException error(String message) {
        return error(message, 0);
    }

    private LexException error(String message, int offset) {
        return new LexException(message, new SourcePos(filename, currentLine, currentColumn - offset));
    }

    public static class LexException extends RuntimeException {
        public final SourcePos pos;
        
        private LexException(String message, SourcePos pos) {
            super(message);
            this.pos = pos;
        }
    }

    public record Result(List<PositionedToken<?>> tokens, Map<String, Optional<String>> metadata) {
    }
}
