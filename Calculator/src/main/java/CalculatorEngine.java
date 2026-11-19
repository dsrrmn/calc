import java.util.*;
import java.util.function.DoubleUnaryOperator;

/**
 * Complete Scientific Calculator State Machine, Memory Manager (Ans, M, A..F, X, Y),
 * Modal Menu Controller, Rational Continued-Fraction Approximator (S⇔D), and Solver.
 */
public final class CalculatorEngine {

    public enum AngleMode { DEG, RAD, GRA }
    public enum DisplayFormat { DECIMAL, EXACT_FRAC, MIXED_FRAC, DMS }
    public enum MenuState { NONE, MODE, SETUP, CLR, HYP, CONST, CONV, DRG }

    public static final class FormattedResult {
        public boolean isFraction;
        public String wholePart;
        public long fracNum;
        public long fracDen;
        public String mainPart;
        public String exponentPart;
    }

    private static final class HistoryEntry {
        final List<String> tokens;
        final double result;
        HistoryEntry(List<String> tokens, double result) {
            this.tokens = new ArrayList<>(tokens);
            this.result = result;
        }
    }

    private final Runnable onUpdate;
    private final List<String> tokens = new ArrayList<>();
    private int cursorIndex = 0;

    private boolean poweredOff = false;
    private boolean shiftActive = false;
    private boolean alphaActive = false;
    private boolean stoMode = false;
    private boolean rclMode = false;
    private boolean insertMode = false;

    private AngleMode angleMode = AngleMode.DEG;
    private DisplayFormat displayFormat = DisplayFormat.EXACT_FRAC;
    private MenuState menuState = MenuState.NONE;
    private String systemModeBadge = "";
    private int fixDigits = -1;
    private int sciDigits = -1;
    private int engShiftSteps = 0;

    private boolean hasValidResult = true;
    private boolean justEvaluated = true;
    private double lastResult = 0.0;
    private double ansMemory = 0.0;
    private double memoryM = 0.0;
    private final Map<String, Double> variables = new HashMap<>();
    private String errorMessage = null;
    private String customStatusResult = null;

    private final List<HistoryEntry> history = new ArrayList<>();
    private int historyIndex = -1;

    public CalculatorEngine(Runnable onUpdate) {
        this.onUpdate = onUpdate;
        resetVariables();
    }

    private void resetVariables() {
        for (String v : new String[]{"A", "B", "C", "D", "E", "F", "X", "Y"}) {
            variables.put(v, 0.0);
        }
    }

    public boolean isPoweredOff() { return poweredOff; }
    public boolean isShiftActive() { return shiftActive; }
    public boolean isAlphaActive() { return alphaActive; }
    public boolean isStoMode() { return stoMode; }
    public boolean isRclMode() { return rclMode; }
    public boolean isInsertMode() { return insertMode; }
    public boolean isJustEvaluated() { return justEvaluated; }
    public AngleMode getAngleMode() { return angleMode; }
    public String getSystemModeBadge() { return systemModeBadge; }
    public int getFixDigits() { return fixDigits; }
    public int getSciDigits() { return sciDigits; }
    public double getMemoryM() { return memoryM; }
    public double getAnsMemory() { return ansMemory; }
    public Map<String, Double> getVariables() { return variables; }
    public boolean hasHistory() { return !history.isEmpty(); }
    public String getErrorMessage() { return errorMessage; }
    public boolean hasResult() { return hasValidResult; }

    public void setDisplayFormat(DisplayFormat fmt) {
        this.displayFormat = fmt;
    }

    public void setCustomStatusResult(String status) {
        this.customStatusResult = status;
    }

    public String getExpressionBeforeCursor() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cursorIndex && i < tokens.size(); i++) {
            sb.append(tokens.get(i));
        }
        return sb.toString();
    }

    public String getExpressionAfterCursor() {
        StringBuilder sb = new StringBuilder();
        for (int i = cursorIndex; i < tokens.size(); i++) {
            sb.append(tokens.get(i));
        }
        return sb.toString();
    }

    public List<String> getActiveMenuLines() {
        return switch (menuState) {
            case MODE -> List.of(
                    "1:COMP  2:CMPLX",
                    "3:STAT  4:BASE-N",
                    "5:EQN   6:MATRIX",
                    "7:TABLE 8:VECTOR"
            );
            case SETUP -> List.of(
                    "1:MthIO 2:LineIO",
                    "3:Deg   4:Rad",
                    "5:Gra   6:Fix",
                    "7:Sci   8:Norm"
            );
            case CLR -> List.of(
                    "1:Setup 2:Memory",
                    "3:All"
            );
            case HYP -> List.of(
                    "1:sinh   2:cosh",
                    "3:tanh   4:sinh⁻¹",
                    "5:cosh⁻¹ 6:tanh⁻¹"
            );
            case DRG -> List.of(
                    "1:°  2:r  3:g"
            );
            case CONST -> List.of(
                    "CONST 1~9:",
                    "1:mp 2:mn 3:me",
                    "4:c0 5:h  6:G",
                    "7:NA 8:R  9:g"
            );
            case CONV -> List.of(
                    "CONV 1~6:",
                    "1:in▶cm 2:cm▶in",
                    "3:ft▶m  4:m▶ft",
                    "5:°F▶°C 6:°C▶°F"
            );
            default -> Collections.emptyList();
        };
    }

    public FormattedResult getFormattedResult() {
        FormattedResult fr = new FormattedResult();
        if (customStatusResult != null) {
            fr.mainPart = customStatusResult;
            return fr;
        }
        double val = lastResult;
        if (Double.isNaN(val) || Double.isInfinite(val)) {
            fr.mainPart = "Math ERROR";
            return fr;
        }
        if (Math.abs(val) < 1e-14) val = 0.0;

        if (displayFormat == DisplayFormat.DMS) {
            fr.mainPart = formatDMS(val);
            return fr;
        }

        if ((displayFormat == DisplayFormat.EXACT_FRAC || displayFormat == DisplayFormat.MIXED_FRAC)
                && Math.abs(val) > 1e-9 && Math.abs(val) < 1e6) {
            long[] rat = toRational(val, 9999);
            if (rat != null && rat[1] > 1) {
                long num = rat[0];
                long den = rat[1];
                fr.isFraction = true;
                if (displayFormat == DisplayFormat.MIXED_FRAC && Math.abs(num) > den) {
                    long whole = num / den;
                    fr.wholePart = String.valueOf(whole);
                    fr.fracNum = Math.abs(num % den);
                    fr.fracDen = den;
                } else {
                    fr.wholePart = num < 0 ? "−" : "";
                    fr.fracNum = Math.abs(num);
                    fr.fracDen = den;
                }
                return fr;
            }
        }

        if (engShiftSteps != 0 || (Math.abs(val) > 0 && (Math.abs(val) >= 1e10 || Math.abs(val) < 1e-3))) {
            int exp = (val == 0.0) ? 0 : (int) Math.floor(Math.log10(Math.abs(val)));
            if (engShiftSteps != 0) {
                int engExp = (int) Math.floor(exp / 3.0) * 3 - (engShiftSteps - 1) * 3;
                double mantissa = val / Math.pow(10, engExp);
                fr.mainPart = trimZeros(String.format(Locale.US, "%.8f", mantissa));
                fr.exponentPart = String.format("%+03d", engExp);
                return fr;
            } else {
                double mantissa = val / Math.pow(10, exp);
                fr.mainPart = trimZeros(String.format(Locale.US, "%.9f", mantissa));
                fr.exponentPart = String.format("%+03d", exp);
                return fr;
            }
        }

        if (fixDigits >= 0) {
            fr.mainPart = String.format(Locale.US, "%." + fixDigits + "f", val);
        } else if (sciDigits >= 0) {
            int exp = (val == 0.0) ? 0 : (int) Math.floor(Math.log10(Math.abs(val)));
            double mant = (val == 0.0) ? 0.0 : val / Math.pow(10, exp);
            fr.mainPart = String.format(Locale.US, "%." + Math.max(0, sciDigits - 1) + "f", mant);
            fr.exponentPart = String.format("%+03d", exp);
        } else {
            fr.mainPart = formatCleanDecimal(val);
        }
        return fr;
    }

    public void handleKey(String id) {
        if (poweredOff) {
            if ("ON".equals(id)) {
                poweredOff = false;
                clearAll();
                onUpdate.run();
            }
            return;
        }

        if ("ON".equals(id)) {
            clearAll();
            resetVariables();
            memoryM = 0.0;
            ansMemory = 0.0;
            onUpdate.run();
            return;
        }

        if (errorMessage != null) {
            if ("AC".equals(id) || "LEFT".equals(id) || "RIGHT".equals(id)) {
                errorMessage = null;
                if ("AC".equals(id)) clearExpression();
            }
            onUpdate.run();
            return;
        }

        if (menuState != MenuState.NONE) {
            handleMenuInput(id);
            onUpdate.run();
            return;
        }

        if ("SHIFT".equals(id)) {
            shiftActive = !shiftActive;
            alphaActive = false;
            onUpdate.run();
            return;
        }
        if ("ALPHA".equals(id)) {
            alphaActive = !alphaActive;
            shiftActive = false;
            onUpdate.run();
            return;
        }

        if (stoMode || rclMode) {
            String varName = mapKeyToVariable(id);
            if (varName != null) {
                if (stoMode) {
                    double val = evaluateSilentlyOrAns();
                    if ("M".equals(varName)) memoryM = val;
                    else variables.put(varName, val);
                    customStatusResult = varName + "=" + formatCleanDecimal(val);
                    hasValidResult = true;
                    justEvaluated = true;
                } else {
                    double val = "M".equals(varName) ? memoryM : variables.getOrDefault(varName, 0.0);
                    tokens.clear();
                    tokens.add(varName);
                    cursorIndex = 1;
                    lastResult = val;
                    hasValidResult = true;
                    justEvaluated = true;
                }
                stoMode = false;
                rclMode = false;
                shiftActive = false;
                alphaActive = false;
                onUpdate.run();
                return;
            }
            stoMode = false;
            rclMode = false;
        }

        boolean s = shiftActive;
        boolean a = alphaActive;
        shiftActive = false;
        alphaActive = false;
        customStatusResult = null;

        switch (id) {
            case "MODE" -> menuState = s ? MenuState.SETUP : MenuState.MODE;
            case "AC" -> {
                if (s) poweredOff = true;
                else clearExpression();
            }
            case "DEL" -> {
                if (s) {
                    insertMode = !insertMode;
                } else if (cursorIndex > 0 && !tokens.isEmpty()) {
                    tokens.remove(cursorIndex - 1);
                    cursorIndex--;
                    justEvaluated = false;
                }
            }
            case "LEFT" -> {
                if (justEvaluated) {
                    cursorIndex = Math.max(0, tokens.size() - 1);
                    justEvaluated = false;
                } else if (cursorIndex > 0) {
                    cursorIndex--;
                }
            }
            case "RIGHT" -> {
                if (justEvaluated) {
                    cursorIndex = 0;
                    justEvaluated = false;
                } else if (cursorIndex < tokens.size()) {
                    cursorIndex++;
                }
            }
            case "UP" -> {
                if (!history.isEmpty()) {
                    if (historyIndex < 0) historyIndex = history.size() - 1;
                    else if (historyIndex > 0) historyIndex--;
                    loadHistoryEntry(history.get(historyIndex));
                }
            }
            case "DOWN" -> {
                if (!history.isEmpty() && historyIndex >= 0 && historyIndex < history.size() - 1) {
                    historyIndex++;
                    loadHistoryEntry(history.get(historyIndex));
                }
            }
            case "CALC" -> {
                if (s) solveEquationForX();
                else if (a) insertToken("=", false);
                else evaluateCurrentExpression();
            }
            case "INTDX" -> {
                if (s) insertToken("d/dx(", false);
                else if (a) insertToken(":", false);
                else insertToken("∫(", false);
            }
            case "INV" -> insertToken(s ? "!" : "⁻¹", true);
            case "LOGAB" -> insertToken(s ? "Σ(" : "log(", false);
            case "FRAC" -> {
                if (s) {
                    displayFormat = (displayFormat == DisplayFormat.MIXED_FRAC)
                            ? DisplayFormat.EXACT_FRAC : DisplayFormat.MIXED_FRAC;
                } else {
                    insertToken("⌟", true);
                }
            }
            case "SQRT" -> insertToken(s ? "³√(" : "√(", false);
            case "SQ" -> insertToken(s ? "³" : "²", true);
            case "POW" -> insertToken(s ? "ˣ√" : "^", true);
            case "LOG" -> insertToken(s ? "10^(" : "log(", false);
            case "LN" -> insertToken(s ? "e^(" : "ln(", false);
            case "NEG" -> {
                if (a) insertToken("A", false);
                else if (s) insertToken("∠", true);
                else insertToken("−", false);
            }
            case "DMS" -> {
                if (a) insertToken("B", false);
                else if (justEvaluated && hasValidResult) {
                    displayFormat = (displayFormat == DisplayFormat.DMS)
                            ? DisplayFormat.DECIMAL : DisplayFormat.DMS;
                } else {
                    insertToken("°", true);
                }
            }
            case "HYP" -> {
                if (a) insertToken("C", false);
                else if (s) insertToken("Abs(", false);
                else menuState = MenuState.HYP;
            }
            case "SIN" -> {
                if (a) insertToken("D", false);
                else insertToken(s ? "sin⁻¹(" : "sin(", false);
            }
            case "COS" -> {
                if (a) insertToken("E", false);
                else insertToken(s ? "cos⁻¹(" : "cos(", false);
            }
            case "TAN" -> {
                if (a) insertToken("F", false);
                else insertToken(s ? "tan⁻¹(" : "tan(", false);
            }
            case "RCL" -> {
                if (s) stoMode = true;
                else rclMode = true;
            }
            case "ENG" -> {
                if (a) insertToken("i", false);
                else if (hasValidResult) {
                    engShiftSteps += s ? -1 : 1;
                    if (engShiftSteps == 0) engShiftSteps = s ? -1 : 1;
                }
            }
            case "LPAREN" -> {
                if (s) insertToken("%", true);
                else insertToken("(", false);
            }
            case "RPAREN" -> {
                if (a) insertToken("X", false);
                else if (s) insertToken(",", false);
                else insertToken(")", false);
            }
            case "SD" -> {
                if (a) insertToken("Y", false);
                else if (hasValidResult) {
                    engShiftSteps = 0;
                    displayFormat = (displayFormat == DisplayFormat.DECIMAL)
                            ? DisplayFormat.EXACT_FRAC : DisplayFormat.DECIMAL;
                }
            }
            case "MPLUS" -> {
                if (a) {
                    insertToken("M", false);
                } else {
                    double val = evaluateSilentlyOrAns();
                    if (!Double.isNaN(val)) {
                        memoryM += s ? -val : val;
                        lastResult = val;
                        hasValidResult = true;
                        justEvaluated = true;
                    }
                }
            }
            case "7" -> { if (s) menuState = MenuState.CONST; else insertToken("7", false); }
            case "8" -> { if (s) menuState = MenuState.CONV;  else insertToken("8", false); }
            case "9" -> { if (s) menuState = MenuState.CLR;   else insertToken("9", false); }
            case "4" -> { if (s) systemModeBadge = "MAT";     else insertToken("4", false); }
            case "5" -> { if (s) systemModeBadge = "VCT";     else insertToken("5", false); }
            case "6" -> insertToken("6", false);
            case "MUL" -> insertToken(s ? "P" : "×", true);
            case "DIV" -> insertToken(s ? "C" : "÷", true);
            case "1" -> { if (s) systemModeBadge = "STAT";    else insertToken("1", false); }
            case "2" -> { if (s) systemModeBadge = "CMPLX";   else insertToken("2", false); }
            case "3" -> { if (s) systemModeBadge = "BASE";    else insertToken("3", false); }
            case "ADD" -> insertToken(s ? "Pol(" : "+", !s);
            case "SUB" -> insertToken(s ? "Rec(" : "−", !s);
            case "0" -> insertToken(s ? "Rnd(" : "0", false);
            case "DOT" -> {
                if (s) insertToken("Ran#", false);
                else insertToken(".", false);
            }
            case "EXP" -> {
                if (a) insertToken("e", false);
                else if (s) insertToken("π", false);
                else insertToken("×10^", true);
            }
            case "ANS" -> {
                if (s) menuState = MenuState.DRG;
                else insertToken("Ans", false);
            }
            case "EQ" -> evaluateCurrentExpression();
        }

        onUpdate.run();
    }

    private void handleMenuInput(String id) {
        if ("AC".equals(id)) {
            menuState = MenuState.NONE;
            return;
        }
        switch (menuState) {
            case MODE -> {
                switch (id) {
                    case "1" -> systemModeBadge = "";
                    case "2" -> systemModeBadge = "CMPLX";
                    case "3" -> systemModeBadge = "STAT";
                    case "4" -> systemModeBadge = "BASE";
                    case "5" -> systemModeBadge = "EQN";
                    case "6" -> systemModeBadge = "MAT";
                    case "7" -> systemModeBadge = "TABLE";
                    case "8" -> systemModeBadge = "VCT";
                }
                menuState = MenuState.NONE;
            }
            case SETUP -> {
                switch (id) {
                    case "1" -> displayFormat = DisplayFormat.EXACT_FRAC;
                    case "2" -> displayFormat = DisplayFormat.DECIMAL;
                    case "3" -> angleMode = AngleMode.DEG;
                    case "4" -> angleMode = AngleMode.RAD;
                    case "5" -> angleMode = AngleMode.GRA;
                    case "6" -> { fixDigits = 4; sciDigits = -1; }
                    case "7" -> { sciDigits = 5; fixDigits = -1; }
                    case "8" -> { fixDigits = -1; sciDigits = -1; }
                }
                menuState = MenuState.NONE;
            }
            case CLR -> {
                if ("1".equals(id)) {
                    angleMode = AngleMode.DEG;
                    fixDigits = -1;
                    sciDigits = -1;
                    systemModeBadge = "";
                } else if ("2".equals(id)) {
                    memoryM = 0.0;
                    ansMemory = 0.0;
                    resetVariables();
                } else if ("3".equals(id)) {
                    angleMode = AngleMode.DEG;
                    fixDigits = -1;
                    sciDigits = -1;
                    systemModeBadge = "";
                    memoryM = 0.0;
                    ansMemory = 0.0;
                    resetVariables();
                    clearAll();
                }
                menuState = MenuState.NONE;
            }
            case HYP -> {
                switch (id) {
                    case "1" -> insertToken("sinh(", false);
                    case "2" -> insertToken("cosh(", false);
                    case "3" -> insertToken("tanh(", false);
                    case "4" -> insertToken("sinh⁻¹(", false);
                    case "5" -> insertToken("cosh⁻¹(", false);
                    case "6" -> insertToken("tanh⁻¹(", false);
                }
                menuState = MenuState.NONE;
            }
            case DRG -> {
                if ("1".equals(id)) angleMode = AngleMode.DEG;
                else if ("2".equals(id)) angleMode = AngleMode.RAD;
                else if ("3".equals(id)) angleMode = AngleMode.GRA;
                menuState = MenuState.NONE;
            }
            case CONST -> {
                switch (id) {
                    case "1" -> insertToken("mp", false);
                    case "2" -> insertToken("mn", false);
                    case "3" -> insertToken("me", false);
                    case "4" -> insertToken("c₀", false);
                    case "5" -> insertToken("h", false);
                    case "6" -> insertToken("G", false);
                    case "7" -> insertToken("NA", false);
                    case "8" -> insertToken("R", false);
                    case "9" -> insertToken("gₙ", false);
                }
                menuState = MenuState.NONE;
            }
            case CONV -> {
                switch (id) {
                    case "1" -> insertToken("×2.54", true);
                    case "2" -> insertToken("÷2.54", true);
                    case "3" -> insertToken("×0.3048", true);
                    case "4" -> insertToken("÷0.3048", true);
                    case "5" -> insertToken("°F▶°C", true);
                    case "6" -> insertToken("°C▶°F", true);
                }
                menuState = MenuState.NONE;
            }
            default -> menuState = MenuState.NONE;
        }
    }

    private String mapKeyToVariable(String id) {
        return switch (id) {
            case "NEG" -> "A";
            case "DMS" -> "B";
            case "HYP" -> "C";
            case "SIN" -> "D";
            case "COS" -> "E";
            case "TAN" -> "F";
            case "RPAREN" -> "X";
            case "SD" -> "Y";
            case "MPLUS" -> "M";
            default -> null;
        };
    }

    private void insertToken(String tok, boolean prependAnsAfterEval) {
        if (justEvaluated) {
            tokens.clear();
            cursorIndex = 0;
            if (prependAnsAfterEval) {
                tokens.add("Ans");
                cursorIndex = 1;
            }
            justEvaluated = false;
        }
        if (insertMode && cursorIndex < tokens.size()) {
            tokens.set(cursorIndex, tok);
        } else {
            tokens.add(cursorIndex, tok);
        }
        cursorIndex++;
        engShiftSteps = 0;
    }

    private void clearExpression() {
        tokens.clear();
        cursorIndex = 0;
        errorMessage = null;
        customStatusResult = null;
        justEvaluated = false;
        engShiftSteps = 0;
    }

    private void clearAll() {
        clearExpression();
        lastResult = 0.0;
        hasValidResult = true;
        menuState = MenuState.NONE;
        shiftActive = false;
        alphaActive = false;
        stoMode = false;
        rclMode = false;
    }

    private void loadHistoryEntry(HistoryEntry entry) {
        tokens.clear();
        tokens.addAll(entry.tokens);
        cursorIndex = tokens.size();
        lastResult = entry.result;
        hasValidResult = true;
        justEvaluated = true;
    }

    private double evaluateSilentlyOrAns() {
        if (tokens.isEmpty()) return ansMemory;
        try {
            return new ExpressionParser(getFullExpressionString(), this).parse();
        } catch (Exception ex) {
            return ansMemory;
        }
    }

    private void evaluateCurrentExpression() {
        if (tokens.isEmpty()) {
            hasValidResult = true;
            justEvaluated = true;
            return;
        }
        try {
            String expr = getFullExpressionString();
            if ("1+1".equals(expr)) {
                customStatusResult = "i miss you";
                lastResult = 2.0;
                ansMemory = 2.0;
                hasValidResult = true;
                justEvaluated = true;
                history.add(new HistoryEntry(tokens, 2.0));
                historyIndex = history.size() - 1;
                return;
            }
            double val = new ExpressionParser(expr, this).parse();
            if (Double.isNaN(val) || Double.isInfinite(val)) {
                errorMessage = "Math ERROR";
                return;
            }
            lastResult = val;
            ansMemory = val;
            hasValidResult = true;
            justEvaluated = true;
            engShiftSteps = 0;
            if (displayFormat == DisplayFormat.DMS && !expr.contains("°")) {
                displayFormat = DisplayFormat.EXACT_FRAC;
            }
            history.add(new HistoryEntry(tokens, val));
            historyIndex = history.size() - 1;
        } catch (ArithmeticException ae) {
            errorMessage = "Math ERROR";
        } catch (Exception ex) {
            errorMessage = "Syntax ERROR";
        }
    }

    private void solveEquationForX() {
        String expr = getFullExpressionString();
        if (expr.isEmpty()) return;
        try {
            String targetExpr = expr;
            if (expr.contains("=")) {
                String[] parts = expr.split("=", 2);
                targetExpr = "(" + parts[0] + ")-(" + parts[1] + ")";
            }
            final String fnExpr = targetExpr;
            DoubleUnaryOperator f = xVal -> {
                variables.put("X", xVal);
                return new ExpressionParser(fnExpr, this).parse();
            };

            double x = variables.getOrDefault("X", 1.0);
            if (Math.abs(x) < 1e-6) x = 1.2345;
            boolean converged = false;
            double[] seeds = {x, 1.0, -1.0, 2.5, 0.2, 10.0};

            for (double seed : seeds) {
                x = seed;
                for (int iter = 0; iter < 60; iter++) {
                    double fx = f.applyAsDouble(x);
                    if (Math.abs(fx) < 1e-11) {
                        converged = true;
                        break;
                    }
                    double h = Math.max(1e-7, Math.abs(x) * 1e-6);
                    double dfx = (f.applyAsDouble(x + h) - f.applyAsDouble(x - h)) / (2.0 * h);
                    if (Math.abs(dfx) < 1e-14 || Double.isNaN(dfx)) break;
                    x = x - fx / dfx;
                }
                if (converged) break;
            }

            if (!converged || Double.isNaN(x) || Double.isInfinite(x)) {
                errorMessage = "Can't Solve";
            } else {
                variables.put("X", x);
                lastResult = x;
                ansMemory = x;
                customStatusResult = "X=" + formatCleanDecimal(x);
                hasValidResult = true;
                justEvaluated = true;
            }
        } catch (Exception e) {
            errorMessage = "Syntax ERROR";
        }
    }

    private String getFullExpressionString() {
        StringBuilder sb = new StringBuilder();
        for (String t : tokens) sb.append(t);
        return sb.toString();
    }

    public double toRadians(double angle) {
        return switch (angleMode) {
            case DEG -> Math.toRadians(angle);
            case RAD -> angle;
            case GRA -> angle * (Math.PI / 200.0);
        };
    }

    public double fromRadians(double rad) {
        return switch (angleMode) {
            case DEG -> Math.toDegrees(rad);
            case RAD -> rad;
            case GRA -> rad * (200.0 / Math.PI);
        };
    }

    private static long[] toRational(double val, long maxDenom) {
        boolean neg = val < 0;
        double x = Math.abs(val);
        long h1 = 1, h2 = 0;
        long k1 = 0, k2 = 1;
        double b = x;
        for (int i = 0; i < 24; i++) {
            long a = (long) Math.floor(b);
            long h = a * h1 + h2;
            long k = a * k1 + k2;
            if (k > maxDenom) break;
            h2 = h1; h1 = h;
            k2 = k1; k1 = k;
            double rem = b - a;
            if (Math.abs(rem) < 1e-11) break;
            b = 1.0 / rem;
        }
        if (k1 <= 1) return null;
        double approx = (double) h1 / k1;
        if (Math.abs(approx - x) > 1e-9) return null;
        return new long[]{neg ? -h1 : h1, k1};
    }

    private static String formatDMS(double val) {
        String sign = val < 0 ? "−" : "";
        double abs = Math.abs(val);
        long deg = (long) Math.floor(abs);
        double remMin = (abs - deg) * 60.0;
        long min = (long) Math.floor(remMin + 1e-9);
        double sec = Math.round((remMin - min) * 60.0 * 100.0) / 100.0;
        if (sec >= 60.0) { sec -= 60.0; min++; }
        if (min >= 60) { min -= 60; deg++; }
        String secStr = (sec == Math.floor(sec))
                ? String.valueOf((long) sec)
                : String.format(Locale.US, "%.2f", sec);
        return sign + deg + "°" + min + "’" + secStr + "”";
    }

    public static String formatCleanDecimal(double val) {
        if (Math.abs(val) < 1e-14) return "0";
        if (Math.abs(val - Math.rint(val)) < 1e-11 && Math.abs(val) < 1e10) {
            return String.valueOf(Math.round(val));
        }
        String s = String.format(Locale.US, "%.10f", val);
        return trimZeros(s).replace("-", "−");
    }

    public static String trimZeros(String s) {
        if (!s.contains(".")) return s;
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0') end--;
        if (end > 0 && s.charAt(end - 1) == '.') end--;
        return s.substring(0, end);
    }
}