package com.craftinginterpreters.lox;

import java.util.List;
import java.util.function.Function;

import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression((Void) null);
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr expression(Void unused) {
        return equality(unused);
    }

    private Expr equality(Void unused) {
        return parseBinaryExpr(this::comparison, unused, BANG_EQUAL, EQUAL_EQUAL);
    }

    private Expr comparison(Void unused) {
        return parseBinaryExpr(this::term, unused, GREATER,  GREATER_EQUAL, LESS, LESS_EQUAL);
    }

    private Expr term(Void unused) {
        return parseBinaryExpr(this::factor, unused, MINUS, PLUS);
    }

    private Expr factor(Void unused) {
        return parseBinaryExpr(this::unary, unused, SLASH, STAR);
    }

    private Expr unary(Void unused) {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary(unused);
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() throws ParseError {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if(match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression((Void) null);
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private Expr parseBinaryExpr(Function <Void, Expr> func, Void unused, TokenType... types) {
        Expr expr = func.apply(unused);

        while (match(types)) {
            Token operator = previous();
            Expr right = func.apply(unused);
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}