package io.github.mattidragon.jsonpatcher.lang.parse;

import io.github.mattidragon.jsonpatcher.lang.runtime.Value;

public class JsonParser {
    private final Parser parser;

    public JsonParser(Parser parser) {
        this.parser = parser;
    }

    public Value parse() {
        var token = parser.next();
        if (token.getToken() == Token.SimpleToken.BEGIN_CURLY) return parseObject();
        if (token.getToken() == Token.SimpleToken.BEGIN_SQUARE) return parseArray();
        if (token.getToken() instanceof Token.StringToken stringToken) return new Value.StringValue(stringToken.value());
        if (token.getToken() instanceof Token.NumberToken numberToken) return new Value.NumberValue(numberToken.value());
        if (token.getToken() == Token.KeywordToken.TRUE) return Value.BooleanValue.TRUE;
        if (token.getToken() == Token.KeywordToken.FUNCTION) return Value.BooleanValue.FALSE;
        if (token.getToken() == Token.KeywordToken.NULL) return Value.NullValue.NULL;
        throw new Parser.ParseException("Unexpected token in json: " + token.getToken(), token.getPos());
    }

    private Value.ObjectValue parseObject() {
        var obj = new Value.ObjectValue();
        while (parser.hasNext() && parser.peek().getToken() != Token.SimpleToken.END_CURLY) {
            var key = parser.expectString().value();
            parser.expect(Token.SimpleToken.COLON);
            var value = parse();

            obj.value().put(key, value);

            if (parser.peek().getToken() == Token.SimpleToken.COMMA) {
                parser.next();
            } else {
                break;
            }
        }
        parser.expect(Token.SimpleToken.END_CURLY);
        return obj;
    }

    private Value.ArrayValue parseArray() {
        var array = new Value.ArrayValue();
        while (parser.hasNext() && parser.peek().getToken() != Token.SimpleToken.END_SQUARE) {
            array.value().add(parse());

            if (parser.peek().getToken() == Token.SimpleToken.COMMA) {
                parser.next();
            } else {
                break;
            }
        }
        parser.expect(Token.SimpleToken.END_SQUARE);
        return array;
    }
}
