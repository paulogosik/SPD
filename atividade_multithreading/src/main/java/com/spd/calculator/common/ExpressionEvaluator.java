package com.spd.calculator.common;

public final class ExpressionEvaluator {

    private ExpressionEvaluator() {
    }

    public static double evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Expressão vazia");
        }

        String sanitized = expression.replace(" ", "");
        return parseExpression(sanitized, new int[]{0});
    }

    private static double parseExpression(String expr, int[] pos) {
        double value = parseTerm(expr, pos);
        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op != '+' && op != '-') {
                break;
            }
            pos[0]++;
            double right = parseTerm(expr, pos);
            value = (op == '+') ? value + right : value - right;
        }
        return value;
    }

    private static double parseTerm(String expr, int[] pos) {
        double value = parseFactor(expr, pos);
        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op != '*' && op != '/') {
                break;
            }
            pos[0]++;
            double right = parseFactor(expr, pos);
            if (op == '*') {
                value *= right;
            } else {
                if (right == 0) {
                    throw new ArithmeticException("Divisão por zero");
                }
                value /= right;
            }
        }
        return value;
    }

    private static double parseFactor(String expr, int[] pos) {
        if (pos[0] >= expr.length()) {
            throw new IllegalArgumentException("Expressão incompleta");
        }

        char c = expr.charAt(pos[0]);

        if (c == '+' || c == '-') {
            pos[0]++;
            double value = parseFactor(expr, pos);
            return (c == '-') ? -value : value;
        }

        if (c == '(') {
            pos[0]++;
            double value = parseExpression(expr, pos);
            if (pos[0] >= expr.length() || expr.charAt(pos[0]) != ')') {
                throw new IllegalArgumentException("Parêntese de fechamento ausente");
            }
            pos[0]++;
            return value;
        }

        return parseNumber(expr, pos);
    }

    private static double parseNumber(String expr, int[] pos) {
        int start = pos[0];
        boolean hasDot = false;

        while (pos[0] < expr.length()) {
            char c = expr.charAt(pos[0]);
            if (Character.isDigit(c)) {
                pos[0]++;
            } else if (c == '.' && !hasDot) {
                hasDot = true;
                pos[0]++;
            } else {
                break;
            }
        }

        if (start == pos[0]) {
            throw new IllegalArgumentException("Número esperado na posição " + start);
        }

        return Double.parseDouble(expr.substring(start, pos[0]));
    }
}
