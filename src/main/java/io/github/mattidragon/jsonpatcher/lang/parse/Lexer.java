package io.github.mattidragon.jsonpatcher.lang.parse;

import io.github.mattidragon.jsonpatcher.lang.PositionedException;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Lexer {
    public static final int TAB_WIDTH = 4;
    private final SourceFile file;
    private final String program;
    private final ArrayList<PositionedToken<?>> tokens = new ArrayList<>();
    private int current = 0;
    private int currentLine = 1;
    private int currentColumn = 1;

    public Lexer(String program, String filename) {
        this.program = program;
        this.file = new SourceFile(filename, program);
    }

    public Result lex() {
        while (hasNext()) {
            var c = next();
            switch (c) {
                case '{', '?', '!', '~', '%',
                        '/', '*', '-', '+', '=',
                        '.', ',', ':', ';', ')',
                        '(', ']', '[', '}', '>',
                        '<', '|', '&', '^', '$',
                        '@'
                        -> readSimpleToken(c);

                case ' ', '\r', '\n', '\t' -> {}

                case '"', '\'' -> readString(c);

                case '#' -> skipComment();

                default -> {
                    if (c >= '0' && c <= '9') readNumber(c);
                    else if (isWordStartChar(c)) readWord(c);
                    else throw error("Unexpected character: %c (0x%x)".formatted(c, (int) c), 1);
                }
            }
        }

        return new Result(tokens);
    }

    private void skipComment() {
        while (hasNext() && peek() != '\n') {
            next();
        }
    }

    private void readSimpleToken(char c) {
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

            case '=' -> readEqualsOptionalToken(Token.SimpleToken.ASSIGN, Token.SimpleToken.EQUALS);
            case '>' -> readEqualsOptionalToken(Token.SimpleToken.GREATER_THAN, Token.SimpleToken.GREATER_THAN_EQUAL);
            case '<' -> readEqualsOptionalToken(Token.SimpleToken.LESS_THAN, Token.SimpleToken.LESS_THAN_EQUAL);

            case '&' -> {
                switch (peek()) {
                    case '&' -> {
                        next();
                        addParsedToken(Token.SimpleToken.DOUBLE_AND, 2);
                    }
                    case '=' -> {
                        next();
                        addParsedToken(Token.SimpleToken.AND_ASSIGN, 2);
                    }
                    default -> addParsedToken(Token.SimpleToken.AND, 1);
                }
            }
            case '|' -> {
                switch (peek()) {
                    case '|' -> {
                        next();
                        addParsedToken(Token.SimpleToken.DOUBLE_OR, 2);
                    }
                    case '=' -> {
                        next();
                        addParsedToken(Token.SimpleToken.OR_ASSIGN, 2);
                    }
                    default -> addParsedToken(Token.SimpleToken.OR, 1);
                }
            }
            case '^' -> readEqualsOptionalToken(Token.SimpleToken.XOR, Token.SimpleToken.XOR_ASSIGN);

            case '+' -> readEqualsOptionalToken(Token.SimpleToken.PLUS, Token.SimpleToken.PLUS_ASSIGN);
            case '-' -> readEqualsOptionalToken(Token.SimpleToken.MINUS, Token.SimpleToken.MINUS_ASSIGN);
            case '*' -> readEqualsOptionalToken(Token.SimpleToken.STAR, Token.SimpleToken.STAR_ASSIGN);
            case '/' -> readEqualsOptionalToken(Token.SimpleToken.SLASH, Token.SimpleToken.SLASH_ASSIGN);
            case '%' -> readEqualsOptionalToken(Token.SimpleToken.PERCENT, Token.SimpleToken.PERCENT_ASSIGN);
            case '~' -> addParsedToken(Token.SimpleToken.TILDE, 1);

            case '!' -> readEqualsOptionalToken(Token.SimpleToken.BANG, Token.SimpleToken.NOT_EQUALS);
            case '?' -> addParsedToken(Token.SimpleToken.QUESTION_MARK, 1);
            case '$' -> addParsedToken(Token.SimpleToken.DOLLAR, 1);
            case '@' -> addParsedToken(Token.SimpleToken.AT_SIGN, 1);
        }
    }

    private void readEqualsOptionalToken(Token.SimpleToken withoutEquals, Token.SimpleToken withEquals) {
        if (peek() == '=') {
            next();
            addParsedToken(withEquals, 2);
        } else {
            addParsedToken(withoutEquals, 1);
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
        while (hasNext() && isWordChar(peek())) {
            string.append(next());
            length++;
        }

        var token = switch (string.toString()) {
            case "true" -> Token.KeywordToken.TRUE;
            case "false" -> Token.KeywordToken.FALSE;
            case "null" -> Token.KeywordToken.NULL;
            case "apply" -> Token.KeywordToken.APPLY;
            case "this" -> Token.KeywordToken.THIS;
            case "if" -> Token.KeywordToken.IF;
            case "else" -> Token.KeywordToken.ELSE;
            case "for" -> Token.KeywordToken.FOR;
            case "in" -> Token.KeywordToken.IN;
            case "var" -> Token.KeywordToken.VAR;
            case "val" -> Token.KeywordToken.VAL;
            case "delete" -> Token.KeywordToken.DELETE;
            case "function" -> Token.KeywordToken.FUNCTION;
            case "return" -> Token.KeywordToken.RETURN;
            case "import" -> Token.KeywordToken.IMPORT;
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
        if (!hasNext()) throw error("Unexpected end of file");
        return program.charAt(current);
    }

    private char next() {
        if (!hasNext()) throw error("Unexpected end of file");
        var c = program.charAt(current++);
        if (c == '\n') {
            currentLine++;
            currentColumn = 0;
        } if (c == '\t') {
            currentColumn += TAB_WIDTH;
        } else {
            currentColumn++;
        }

        return c;
    }

    private void addParsedToken(Token token, int length) {
        var from = new SourcePos(file, currentLine, currentColumn - length);
        var to = new SourcePos(file, currentLine, currentColumn - 1);
        tokens.add(PositionedToken.of(new SourceSpan(from, to), token));
    }
    
    private LexException error(String message) {
        return error(message, 0);
    }

    private LexException error(String message, int offset) {
        return new LexException(message, new SourcePos(file, currentLine, currentColumn - offset));
    }

    public static class LexException extends PositionedException {
        public final SourcePos pos;
        
        public LexException(String message, SourcePos pos) {
            super(message);
            this.pos = pos;
        }

        @Override
        protected String getBaseMessage() {
            return "Error while parsing tokens";
        }

        @Override
        protected @Nullable SourceSpan getPos() {
            return new SourceSpan(pos, pos);
        }
    }

    public record Result(List<PositionedToken<?>> tokens) {
    }
}
