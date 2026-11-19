package com.calculator.app;

public class MathEvaluator {

    private String input;
    private int pos;
    private int ch;

    public double evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            return 0.0;
        }
        this.input = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace(" ", "");
        this.pos = -1;
        nextChar();
        double result = parseExpression();
        if (pos < input.length()) {
            throw new IllegalArgumentException("Unexpected character: " + (char) ch);
        }
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            throw new ArithmeticException("Math error");
        }
        return result;
    }

    private void nextChar() {
        pos++;
        ch = (pos < input.length()) ? input.charAt(pos) : -1;
    }

    private boolean eat(int charToEat) {
        while (ch == ' ') {
            nextChar();
        }
        if (ch == charToEat) {
            nextChar();
            return true;
        }
        return false;
    }

    private double parseExpression() {
        double x = parseTerm();
        while (true) {
            if (eat('+')) {
                x += parseTerm();
            } else if (eat('-')) {
                x -= parseTerm();
            } else {
                return x;
            }
        }
    }

    private double parseTerm() {
        double x = parseFactor();
        while (true) {
            if (eat('*')) {
                x *= parseFactor();
            } else if (eat('/')) {
                double divisor = parseFactor();
                if (divisor == 0.0) {
                    throw new ArithmeticException("Division by zero");
                }
                x /= divisor;
            } else if (eat('%')) {
                if ( isNextNumberOrFactor() ) {
                    double mod = parseFactor();
                    if (mod == 0.0) {
                        throw new ArithmeticException("Modulo by zero");
                    }
                    x %= mod;
                } else {
                    x /= 100.0;
                }
            } else if (isImplicitMultiplicationStart()) {
                x *= parseFactor();
            } else {
                return x;
            }
        }
    }

    private boolean isNextNumberOrFactor() {
        return (ch >= '0' && ch <= '9') || ch == '.' || ch == '(' || (ch >= 'a' && ch <= 'z');
    }

    private boolean isImplicitMultiplicationStart() {
        return ch == '(' || (ch >= 'a' && ch <= 'z');
    }

    private double parseFactor() {
        if (eat('+')) {
            return parseFactor();
        }
        if (eat('-')) {
            return -parseFactor();
        }

        double x;
        int startPos = this.pos;

        if (eat('(')) {
            x = parseExpression();
            if (!eat(')')) {
                throw new IllegalArgumentException("Missing closing parenthesis");
            }
        } else if ((ch >= '0' && ch <= '9') || ch == '.') {
            while ((ch >= '0' && ch <= '9') || ch == '.') {
                nextChar();
            }
            x = Double.parseDouble(input.substring(startPos, this.pos));
        } else if (ch >= 'a' && ch <= 'z') {
            while (ch >= 'a' && ch <= 'z') {
                nextChar();
            }
            String identifier = input.substring(startPos, this.pos);
            if (identifier.equals("pi")) {
                x = Math.PI;
            } else if (identifier.equals("e")) {
                x = Math.E;
            } else {
                double arg = parseFactor();
                x = applyFunction(identifier, arg);
            }
        } else {
            throw new IllegalArgumentException("Unexpected token");
        }

        if (eat('^')) {
            x = Math.pow(x, parseFactor());
        }

        return x;
    }

    private double applyFunction(String func, double arg) {
        return switch (func) {
            case "sin" -> Math.sin(Math.toRadians(arg));
            case "cos" -> Math.cos(Math.toRadians(arg));
            case "tan" -> {
                double modAngle = Math.abs(arg % 180.0);
                if (Math.abs(modAngle - 90.0) < 1e-9) {
                    throw new ArithmeticException("Undefined tangent");
                }
                yield Math.tan(Math.toRadians(arg));
            }
            case "log" -> {
                if (arg <= 0.0) {
                    throw new ArithmeticException("Invalid log domain");
                }
                yield Math.log10(arg);
            }
            case "ln" -> {
                if (arg <= 0.0) {
                    throw new ArithmeticException("Invalid ln domain");
                }
                yield Math.log(arg);
            }
            case "sqrt" -> {
                if (arg < 0.0) {
                    throw new ArithmeticException("Negative square root");
                }
                yield Math.sqrt(arg);
            }
            case "abs" -> Math.abs(arg);
            default -> throw new IllegalArgumentException("Unknown function: " + func);
        };
    }
}
