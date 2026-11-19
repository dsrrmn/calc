/**
 * Complete Recursive-Descent Scientific Expression Parser supporting operator precedence,
 * implicit multiplication, trig/hyperbolic functions, logarithms, roots, factorials,
 * permutations/combinations, definite integrals, derivatives, summations, and Pol/Rec.
 */
public final class ExpressionParser {

    private final String s;
    private final CalculatorEngine env;
    private int pos = 0;

    public ExpressionParser(String s, CalculatorEngine env) {
        this.s = s.replace("−", "-");
        this.env = env;
    }

    public double parse() {
        double val = parseColonStatements();
        skipWhitespace();
        if (pos < s.length()) {
            throw new IllegalArgumentException("Unexpected token at " + pos);
        }
        return val;
    }

    private double parseColonStatements() {
        double v = parseComparison();
        while (match(':')) {
            v = parseComparison();
        }
        return v;
    }

    private double parseComparison() {
        double v = parseAddSub();
        if (match('=')) {
            v = v - parseAddSub();
        }
        return v;
    }

    private double parseAddSub() {
        double v = parseMulDiv();
        while (true) {
            skipWhitespace();
            if (match('+')) {
                v += parseMulDiv();
            } else if (match('-')) {
                v -= parseMulDiv();
            } else {
                break;
            }
        }
        return v;
    }

    private double parseMulDiv() {
        double v = parsePermComb();
        while (true) {
            skipWhitespace();
            if (match('×') || match('*')) {
                v *= parsePermComb();
            } else if (match('÷') || match('/')) {
                double denom = parsePermComb();
                if (Math.abs(denom) < 1e-15) throw new ArithmeticException("Div by 0");
                v /= denom;
            } else if (matchStr("∠")) {
                double theta = env.toRadians(parsePermComb());
                v = v * Math.cos(theta);
            } else if (isImplicitMultiplyStart()) {
                v *= parsePermComb();
            } else {
                break;
            }
        }
        return v;
    }

    private boolean isImplicitMultiplyStart() {
        if (pos >= s.length()) return false;
        char c = s.charAt(pos);
        return c == '(' || c == 'π' || c == '√' || c == '³' || c == '∫' || c == 'Σ'
                || Character.isLetter(c);
    }

    private double parsePermComb() {
        double v = parseUnary();
        while (true) {
            skipWhitespace();
            if (match('P')) {
                double r = parseUnary();
                v = nPr(v, r);
            } else if (match('C')) {
                double r = parseUnary();
                v = nCr(v, r);
            } else {
                break;
            }
        }
        return v;
    }

    private double parseUnary() {
        skipWhitespace();
        if (match('+')) return parseUnary();
        if (match('-')) return -parseUnary();
        return parsePowerAndRoot();
    }

    private double parsePowerAndRoot() {
        double base = parsePostfix();
        skipWhitespace();
        if (match('^')) {
            double exp = parseUnary();
            return Math.pow(base, exp);
        } else if (matchStr("ˣ√")) {
            double radicand = parseUnary();
            if (Math.abs(base) < 1e-15) throw new ArithmeticException("Zero root");
            if (radicand < 0 && Math.abs(base % 2.0 - 1.0) < 1e-9) {
                return -Math.pow(-radicand, 1.0 / base);
            }
            return Math.pow(radicand, 1.0 / base);
        } else if (matchStr("×10^")) {
            double exp = parseUnary();
            return base * Math.pow(10.0, exp);
        }
        return base;
    }

    private double parsePostfix() {
        double v = parseFraction();
        while (true) {
            skipWhitespace();
            if (matchStr("⁻¹")) {
                if (Math.abs(v) < 1e-15) throw new ArithmeticException("Div by 0");
                v = 1.0 / v;
            } else if (match('²')) {
                v = v * v;
            } else if (match('³') && !peekStr("√(")) {
                v = v * v * v;
            } else if (match('!')) {
                v = factorial(v);
            } else if (match('%')) {
                v = v * 0.01;
            } else if (matchStr("°F▶°C")) {
                v = (v - 32.0) * (5.0 / 9.0);
            } else if (matchStr("°C▶°F")) {
                v = v * 1.8 + 32.0;
            } else if (match('°')) {
                env.setDisplayFormat(CalculatorEngine.DisplayFormat.DMS);
                while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) {
                    double mins = readNumberLiteral();
                    match('°');
                    v += (v >= 0 ? 1 : -1) * (mins / 60.0);
                    if (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) {
                        double secs = readNumberLiteral();
                        match('°');
                        v += (v >= 0 ? 1 : -1) * (secs / 3600.0);
                    }
                }
            } else {
                break;
            }
        }
        return v;
    }

    private double parseFraction() {
        double a = parsePrimary();
        if (match('⌟')) {
            double b = parsePrimary();
            if (match('⌟')) {
                double c = parsePrimary();
                if (Math.abs(c) < 1e-15) throw new ArithmeticException("Div by 0");
                return a >= 0 ? (a + b / c) : (a - b / c);
            }
            if (Math.abs(b) < 1e-15) throw new ArithmeticException("Div by 0");
            return a / b;
        }
        return a;
    }

    private double parsePrimary() {
        skipWhitespace();
        if (pos >= s.length()) throw new IllegalArgumentException("Unexpected end");

        if (match('(')) {
            double v = parseColonStatements();
            match(')');
            return v;
        }

        if (matchStr("∫(")) return parseIntegral();
        if (matchStr("d/dx(")) return parseDerivative();
        if (matchStr("Σ(")) return parseSummation();
        if (matchStr("Pol(")) return parsePolRec(true);
        if (matchStr("Rec(")) return parsePolRec(false);

        if (matchStr("sinh⁻¹(")) { double x = parseArgClose(); return Math.log(x + Math.sqrt(x * x + 1.0)); }
        if (matchStr("cosh⁻¹(")) { double x = parseArgClose(); if (x < 1.0) throw new ArithmeticException(); return Math.log(x + Math.sqrt(x * x - 1.0)); }
        if (matchStr("tanh⁻¹(")) { double x = parseArgClose(); if (Math.abs(x) >= 1.0) throw new ArithmeticException(); return 0.5 * Math.log((1.0 + x) / (1.0 - x)); }
        if (matchStr("sinh(")) return Math.sinh(parseArgClose());
        if (matchStr("cosh(")) return Math.cosh(parseArgClose());
        if (matchStr("tanh(")) return Math.tanh(parseArgClose());

        if (matchStr("sin⁻¹(")) { double x = parseArgClose(); if (Math.abs(x) > 1.0) throw new ArithmeticException(); return env.fromRadians(Math.asin(x)); }
        if (matchStr("cos⁻¹(")) { double x = parseArgClose(); if (Math.abs(x) > 1.0) throw new ArithmeticException(); return env.fromRadians(Math.acos(x)); }
        if (matchStr("tan⁻¹(")) return env.fromRadians(Math.atan(parseArgClose()));

        if (matchStr("sin(")) return cleanTrig(Math.sin(env.toRadians(parseArgClose())));
        if (matchStr("cos(")) return cleanTrig(Math.cos(env.toRadians(parseArgClose())));
        if (matchStr("tan(")) {
            double rad = env.toRadians(parseArgClose());
            if (Math.abs(Math.cos(rad)) < 1e-14) throw new ArithmeticException("Tan 90");
            return cleanTrig(Math.tan(rad));
        }

        if (matchStr("log(")) {
            double first = parseAddSub();
            if (match(',')) {
                double second = parseAddSub();
                match(')');
                if (first <= 0 || first == 1.0 || second <= 0) throw new ArithmeticException();
                return Math.log(second) / Math.log(first);
            }
            match(')');
            if (first <= 0) throw new ArithmeticException();
            return Math.log10(first);
        }
        if (matchStr("ln(")) {
            double x = parseArgClose();
            if (x <= 0) throw new ArithmeticException();
            return Math.log(x);
        }
        if (matchStr("10^(")) return Math.pow(10.0, parseArgClose());
        if (matchStr("e^(")) return Math.exp(parseArgClose());
        if (matchStr("³√(")) return Math.cbrt(parseArgClose());
        if (matchStr("√(")) {
            double x = parseArgClose();
            if (x < 0) throw new ArithmeticException();
            return Math.sqrt(x);
        }
        if (matchStr("Abs(")) return Math.abs(parseArgClose());
        if (matchStr("Rnd(")) {
            double x = parseArgClose();
            return Math.round(x * 1e10) / 1e10;
        }

        if (matchStr("Ans")) return env.getAnsMemory();
        if (matchStr("Ran#")) return Math.round(Math.random() * 1000.0) / 1000.0;
        if (matchStr("mp")) return 1.672621898e-27;
        if (matchStr("mn")) return 1.674927471e-27;
        if (matchStr("me")) return 9.10938356e-31;
        if (matchStr("c₀")) return 299792458.0;
        if (matchStr("NA")) return 6.022140857e23;
        if (matchStr("gₙ")) return 9.80665;
        if (match('h')) return 6.62607004e-34;
        if (match('G')) return 6.67408e-11;
        if (match('R')) return 8.3144598;
        if (match('π')) return Math.PI;
        if (match('e')) return Math.E;
        if (match('i')) return 1.0;
        if (match('M')) return env.getMemoryM();

        for (String var : new String[]{"A", "B", "C", "D", "E", "F", "X", "Y"}) {
            if (matchStr(var)) return env.getVariables().getOrDefault(var, 0.0);
        }

        if (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) {
            return readNumberLiteral();
        }

        throw new IllegalArgumentException("Unexpected character: " + s.charAt(pos));
    }

    private double parseArgClose() {
        double v = parseColonStatements();
        match(')');
        return v;
    }

    private double parseIntegral() {
        String body = readSubExpressionUntilComma();
        double a = parseAddSub();
        double b = match(',') ? parseAddSub() : a + 1.0;
        match(')');

        double savedX = env.getVariables().getOrDefault("X", 0.0);
        try {
            int n = 100;
            double h = (b - a) / n;
            double sum = evalAtX(body, a) + evalAtX(body, b);
            for (int i = 1; i < n; i++) {
                double x = a + i * h;
                sum += (i % 2 == 0 ? 2.0 : 4.0) * evalAtX(body, x);
            }
            return sum * (h / 3.0);
        } finally {
            env.getVariables().put("X", savedX);
        }
    }

    private double parseDerivative() {
        String body = readSubExpressionUntilComma();
        double x0 = parseAddSub();
        match(')');

        double savedX = env.getVariables().getOrDefault("X", 0.0);
        try {
            double h = Math.max(1e-6, Math.abs(x0) * 1e-6);
            return (evalAtX(body, x0 + h) - evalAtX(body, x0 - h)) / (2.0 * h);
        } finally {
            env.getVariables().put("X", savedX);
        }
    }

    private double parseSummation() {
        String body = readSubExpressionUntilComma();
        int start = (int) Math.round(parseAddSub());
        int end = match(',') ? (int) Math.round(parseAddSub()) : start;
        match(')');
        if (end < start || end - start > 10000) throw new ArithmeticException();

        double savedX = env.getVariables().getOrDefault("X", 0.0);
        try {
            double sum = 0.0;
            for (int k = start; k <= end; k++) {
                sum += evalAtX(body, k);
            }
            return sum;
        } finally {
            env.getVariables().put("X", savedX);
        }
    }

    private double parsePolRec(boolean isPol) {
        double a = parseAddSub();
        double b = match(',') ? parseAddSub() : 0.0;
        match(')');
        if (isPol) {
            double r = Math.hypot(a, b);
            double theta = env.fromRadians(Math.atan2(b, a));
            env.getVariables().put("X", r);
            env.getVariables().put("Y", theta);
            env.setCustomStatusResult("r=" + CalculatorEngine.formatCleanDecimal(r));
            return r;
        } else {
            double rad = env.toRadians(b);
            double x = a * Math.cos(rad);
            double y = a * Math.sin(rad);
            env.getVariables().put("X", x);
            env.getVariables().put("Y", y);
            env.setCustomStatusResult("X=" + CalculatorEngine.formatCleanDecimal(x));
            return x;
        }
    }

    private String readSubExpressionUntilComma() {
        int start = pos;
        int depth = 0;
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == '(') depth++;
            else if (c == ')') {
                if (depth == 0) break;
                depth--;
            } else if (c == ',' && depth == 0) {
                String sub = s.substring(start, pos);
                pos++;
                return sub;
            }
            pos++;
        }
        return s.substring(start, pos);
    }

    private double evalAtX(String expr, double xVal) {
        env.getVariables().put("X", xVal);
        return new ExpressionParser(expr, env).parse();
    }

    private double readNumberLiteral() {
        int start = pos;
        boolean seenDot = false;
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (Character.isDigit(c)) {
                pos++;
            } else if (c == '.' && !seenDot) {
                seenDot = true;
                pos++;
            } else {
                break;
            }
        }
        return Double.parseDouble(s.substring(start, pos));
    }

    private void skipWhitespace() {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
    }

    private boolean match(char expected) {
        skipWhitespace();
        if (pos < s.length() && s.charAt(pos) == expected) {
            pos++;
            return true;
        }
        return false;
    }

    private boolean matchStr(String expected) {
        skipWhitespace();
        if (s.startsWith(expected, pos)) {
            pos += expected.length();
            return true;
        }
        return false;
    }

    private boolean peekStr(String expected) {
        return s.startsWith(expected, pos);
    }

    private static double cleanTrig(double v) {
        return Math.abs(v) < 1e-14 ? 0.0 : v;
    }

    private static double factorial(double n) {
        if (n < 0 || n != Math.floor(n) || n > 170) throw new ArithmeticException("Factorial domain");
        double res = 1.0;
        for (int i = 2; i <= (int) n; i++) res *= i;
        return res;
    }

    private static double nPr(double n, double r) {
        if (n < 0 || r < 0 || r > n || n != Math.floor(n) || r != Math.floor(r)) {
            throw new ArithmeticException("nPr domain");
        }
        double res = 1.0;
        for (int i = 0; i < (int) r; i++) res *= (n - i);
        return res;
    }

    private static double nCr(double n, double r) {
        if (n < 0 || r < 0 || r > n || n != Math.floor(n) || r != Math.floor(r)) {
            throw new ArithmeticException("nCr domain");
        }
        int k = (int) Math.min(r, n - r);
        double res = 1.0;
        for (int i = 1; i <= k; i++) {
            res = res * (n - k + i) / i;
        }
        return Math.round(res);
    }
}