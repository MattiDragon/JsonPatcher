package io.github.mattidragon.jsonpatcher.lang.parse;

import io.github.mattidragon.jsonpatcher.lang.parse.Token.SimpleToken;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class TokenTree {
    private static final Node TREE = branch()
            .add('{', SimpleToken.BEGIN_CURLY)
            .add('}', SimpleToken.END_CURLY)
            .add('[', SimpleToken.BEGIN_SQUARE)
            .add(']', SimpleToken.END_SQUARE)
            .add('(', SimpleToken.BEGIN_PAREN)
            .add(')', SimpleToken.END_PAREN)
            .add(';', SimpleToken.SEMICOLON)
            .add(':', SimpleToken.COLON)
            .add(',', SimpleToken.COMMA)
            .add('.', SimpleToken.DOT)

            .add('=', SimpleToken.ASSIGN,
                    branch().add('=', SimpleToken.EQUALS))
            .add('<', SimpleToken.LESS_THAN,
                    branch().add('=', SimpleToken.LESS_THAN_EQUAL))
            .add('>', SimpleToken.GREATER_THAN,
                    branch().add('=', SimpleToken.GREATER_THAN_EQUAL))

            .add('&', SimpleToken.AND,
                    branch().add('&', SimpleToken.DOUBLE_AND)
                            .add('=', SimpleToken.AND_ASSIGN))
            .add('|', SimpleToken.OR,
                    branch().add('|', SimpleToken.DOUBLE_OR)
                            .add('=', SimpleToken.OR_ASSIGN))
            .add('^', SimpleToken.XOR,
                    branch().add('=', SimpleToken.XOR_ASSIGN))

            .add('+', SimpleToken.PLUS,
                    branch().add('=', SimpleToken.PLUS_ASSIGN)
                            .add('+', SimpleToken.DOUBLE_PLUS))
            .add('-', SimpleToken.MINUS,
                    branch().add('=', SimpleToken.MINUS_ASSIGN)
                            .add('-', SimpleToken.DOUBLE_MINUS)
                            .add('>', SimpleToken.ARROW))
            .add('*', SimpleToken.STAR,
                    branch().add('=', SimpleToken.STAR_ASSIGN)
                            .add('*', SimpleToken.DOUBLE_STAR))
            .add('/', SimpleToken.SLASH,
                    branch().add('=', SimpleToken.SLASH_ASSIGN))
            .add('%', SimpleToken.PERCENT,
                    branch().add('=', SimpleToken.PERCENT_ASSIGN))
            .add('~', SimpleToken.TILDE)

            .add('!', SimpleToken.BANG,
                    branch().add('=', SimpleToken.NOT_EQUALS)
                            .add('!', SimpleToken.DOUBLE_BANG))
            .add('?', SimpleToken.QUESTION_MARK)
            .add('$', SimpleToken.DOLLAR)
            .add('@', SimpleToken.AT_SIGN)
            .build();

    public static boolean isStart(char c) {
        return TREE.children().containsKey(c);
    }

    public static boolean parse(Lexer lexer, char c) {
        var node = TREE;
        var depth = 1;

        Lexer.Position candidatePos = lexer.savePos();
        SimpleToken candidate = null;
        var candidateDepth = 0;

        for (var currentChar = c; currentChar != 0; currentChar = lexer.hasNext() ? lexer.next() : 0, depth++) {
            if (node.children().containsKey(currentChar)) {
                node = node.children().get(currentChar);

                if (node.token() != null) {
                    candidate = node.token();
                    candidatePos = lexer.savePos();
                    candidateDepth = depth;
                }
            } else {
                break;
            }
        }

        // Either we load the candidate or the original position
        lexer.loadPos(candidatePos);
        if (candidate != null) {
            lexer.addParsedToken(candidate, candidateDepth);
            return true;
        }
        return false;
    }

    private static Builder branch() {
        return new Builder();
    }

    public record Node(@Nullable SimpleToken token, Map<Character, Node> children) {
    }

    private static class Builder {
        private final Map<Character, Node> children = new HashMap<>();

        public Builder add(char c, SimpleToken token) {
            children.put(c, new Node(token, Map.of()));
            return this;
        }

        public Builder add(char c, Builder branch) {
            children.put(c, branch.build());
            return this;
        }

        public Builder add(char c, SimpleToken token, Builder branch) {
            children.put(c, new Node(token, Map.copyOf(branch.children)));
            return this;
        }

        public Node build() {
            return new Node(null, Map.copyOf(children));
        }
    }
}
