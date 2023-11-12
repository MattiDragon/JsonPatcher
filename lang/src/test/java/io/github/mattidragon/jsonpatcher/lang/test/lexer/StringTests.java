package io.github.mattidragon.jsonpatcher.lang.test.lexer;

import io.github.mattidragon.jsonpatcher.lang.parse.Lexer;
import io.github.mattidragon.jsonpatcher.lang.parse.PositionedToken;
import io.github.mattidragon.jsonpatcher.lang.parse.Token;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StringTests {
    @Test
    public void testSimpleUnicodeEscape() {
        var program = """
                "\\u0041"
                """;
        var lexer = new Lexer(program, "test file");
        var tokens = lexer.lex().tokens();
        assertEquals(1, tokens.size(), "Expected 1 token");
        var token = tokens.get(0);
        assertTrue(token instanceof PositionedToken.StringToken, "Expected StringToken");
        assertEquals("A", ((PositionedToken.StringToken) token).getToken().value(), "Expected A");
    }

    @Test
    public void testInvalidUnicodeEscape() {
        var program = """
                "\\u0gggg"
                """;
        var lexer = new Lexer(program, "test file");
        assertThrowsExactly(Lexer.LexException.class, lexer::lex, "Expected exception from invalid escape");
    }

    @Test
    public void testKeywordDetection() {
        var program = """
                true
                """;
        var lexer = new Lexer(program, "test file");
        var tokens = lexer.lex().tokens();
        assertEquals(1, tokens.size(), "Expected 1 token");
        var token = tokens.get(0);
        assertTrue(token instanceof PositionedToken.KeywordToken, "Expected KeywordToken");
        assertEquals(Token.KeywordToken.TRUE, ((PositionedToken.KeywordToken) token).getToken(), "Expected true");
    }

    @Test
    public void testKeywordEscaping() {
        var program = """
                'true'
                """;
        var lexer = new Lexer(program, "test file");
        var tokens = lexer.lex().tokens();
        assertEquals(1, tokens.size(), "Expected 1 token");
        var token = tokens.get(0);
        assertTrue(token instanceof PositionedToken.WordToken, "Expected WordToken");
        assertEquals("true", ((PositionedToken.WordToken) token).getToken().value(), "Expected true");
    }
}
