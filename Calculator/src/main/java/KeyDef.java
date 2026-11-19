import java.util.ArrayList;
import java.util.List;

/**
 * Key definitions, physical coordinates, and secondary silkscreen labels
 * for the Casio fx-991ES PLUS (2nd Edition).
 */
public final class KeyDef {

    public enum KeyCategory {
        SILVER_ROUND, DPAD_ROCKER, SCI_PILL, MAIN_GREY, MAIN_GREEN
    }

    public final String id;
    public final String primaryLabel;
    public final String shiftLabel;
    public final String alphaLabel;
    public final KeyCategory category;
    public final double x, y, w, h;
    public final int dpadDir; // 0=UP, 1=RIGHT, 2=DOWN, 3=LEFT, -1=N/A

    public KeyDef(String id, String primaryLabel, String shiftLabel, String alphaLabel,
                  KeyCategory category, double x, double y, double w, double h, int dpadDir) {
        this.id = id;
        this.primaryLabel = primaryLabel;
        this.shiftLabel = shiftLabel;
        this.alphaLabel = alphaLabel;
        this.category = category;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.dpadDir = dpadDir;
    }

    public static List<KeyDef> createAllKeys() {
        List<KeyDef> keys = new ArrayList<>();
        double[] sciCols = {-55.0, -33.0, -11.0, 11.0, 33.0, 55.0};
        double[] mainCols = {-52.0, -26.0, 0.0, 26.0, 52.0};

        // Top Control Row: 4 Brushed-Metallic Silver Buttons + 4-Way REPLAY D-Pad
        double yCtrl = -33.0;
        keys.add(new KeyDef("SHIFT", "", "SHIFT", "", KeyCategory.SILVER_ROUND, sciCols[0], yCtrl, 13.6, 13.6, -1));
        keys.add(new KeyDef("ALPHA", "", "", "ALPHA", KeyCategory.SILVER_ROUND, sciCols[1], yCtrl, 13.6, 13.6, -1));
        keys.add(new KeyDef("MODE", "", "SETUP", "MODE", KeyCategory.SILVER_ROUND, sciCols[4], yCtrl, 13.6, 13.6, -1));
        keys.add(new KeyDef("ON", "", "", "ON", KeyCategory.SILVER_ROUND, sciCols[5], yCtrl, 13.6, 13.6, -1));

        // Central 4-Way REPLAY D-Pad Rockers (Center at X = 0, Y = -23.0)
        double dpadY = -23.0;
        keys.add(new KeyDef("UP", "▲", "", "", KeyCategory.DPAD_ROCKER, 0, dpadY, 36.0, 36.0, 0));
        keys.add(new KeyDef("RIGHT", "▶", "", "", KeyCategory.DPAD_ROCKER, 0, dpadY, 36.0, 36.0, 1));
        keys.add(new KeyDef("DOWN", "▼", "", "", KeyCategory.DPAD_ROCKER, 0, dpadY, 36.0, 36.0, 2));
        keys.add(new KeyDef("LEFT", "◀", "", "", KeyCategory.DPAD_ROCKER, 0, dpadY, 36.0, 36.0, 3));

        // Row 1 (Sci): CALC (SOLVE), ∫dx (d/dx), [D-Pad Space], x⁻¹ (x!), log□ (Σ)
        double ySci1 = -13.0, sw = 18.2, sh = 10.2;
        keys.add(new KeyDef("CALC", "CALC", "SOLVE", "=", KeyCategory.SCI_PILL, sciCols[0], ySci1, sw, sh, -1));
        keys.add(new KeyDef("INTDX", "∫dx", "d/dx", ":", KeyCategory.SCI_PILL, sciCols[1], ySci1, sw, sh, -1));
        keys.add(new KeyDef("INV", "x⁻¹", "x!", "", KeyCategory.SCI_PILL, sciCols[4], ySci1, sw, sh, -1));
        keys.add(new KeyDef("LOGAB", "log□", "Σ", "", KeyCategory.SCI_PILL, sciCols[5], ySci1, sw, sh, -1));

        // Row 2 (Sci): a b/c (d/c), √ (³√), x² (x³), x■ (ˣ√), log (10ˣ), ln (eˣ)
        double ySci2 = 9.0;
        keys.add(new KeyDef("FRAC", "a b/c", "d/c", "", KeyCategory.SCI_PILL, sciCols[0], ySci2, sw, sh, -1));
        keys.add(new KeyDef("SQRT", "√", "³√", "", KeyCategory.SCI_PILL, sciCols[1], ySci2, sw, sh, -1));
        keys.add(new KeyDef("SQ", "x²", "x³", "", KeyCategory.SCI_PILL, sciCols[2], ySci2, sw, sh, -1));
        keys.add(new KeyDef("POW", "x■", "ˣ√", "", KeyCategory.SCI_PILL, sciCols[3], ySci2, sw, sh, -1));
        keys.add(new KeyDef("LOG", "log", "10ˣ", "", KeyCategory.SCI_PILL, sciCols[4], ySci2, sw, sh, -1));
        keys.add(new KeyDef("LN", "ln", "eˣ", "", KeyCategory.SCI_PILL, sciCols[5], ySci2, sw, sh, -1));

        // Row 3 (Sci): (-) (∠ | A), °'" (← | B), hyp (Abs | C), sin (sin⁻¹ | D), cos (cos⁻¹ | E), tan (tan⁻¹ | F)
        double ySci3 = 29.0;
        keys.add(new KeyDef("NEG", "(-)", "∠", "A", KeyCategory.SCI_PILL, sciCols[0], ySci3, sw, sh, -1));
        keys.add(new KeyDef("DMS", "° ' \"", "←", "B", KeyCategory.SCI_PILL, sciCols[1], ySci3, sw, sh, -1));
        keys.add(new KeyDef("HYP", "hyp", "Abs", "C", KeyCategory.SCI_PILL, sciCols[2], ySci3, sw, sh, -1));
        keys.add(new KeyDef("SIN", "sin", "sin⁻¹", "D", KeyCategory.SCI_PILL, sciCols[3], ySci3, sw, sh, -1));
        keys.add(new KeyDef("COS", "cos", "cos⁻¹", "E", KeyCategory.SCI_PILL, sciCols[4], ySci3, sw, sh, -1));
        keys.add(new KeyDef("TAN", "tan", "tan⁻¹", "F", KeyCategory.SCI_PILL, sciCols[5], ySci3, sw, sh, -1));

        // Row 4 (Sci): RCL (STO), ENG (← | i), ( (%), ) (, | X), S⇔D (a+bi | Y), M+ (M- | M)
        double ySci4 = 49.0;
        keys.add(new KeyDef("RCL", "RCL", "STO", "", KeyCategory.SCI_PILL, sciCols[0], ySci4, sw, sh, -1));
        keys.add(new KeyDef("ENG", "ENG", "←", "i", KeyCategory.SCI_PILL, sciCols[1], ySci4, sw, sh, -1));
        keys.add(new KeyDef("LPAREN", "(", "%", "", KeyCategory.SCI_PILL, sciCols[2], ySci4, sw, sh, -1));
        keys.add(new KeyDef("RPAREN", ")", ",", "X", KeyCategory.SCI_PILL, sciCols[3], ySci4, sw, sh, -1));
        keys.add(new KeyDef("SD", "S⇔D", "a+bi", "Y", KeyCategory.SCI_PILL, sciCols[4], ySci4, sw, sh, -1));
        keys.add(new KeyDef("MPLUS", "M+", "M-", "M", KeyCategory.SCI_PILL, sciCols[5], ySci4, sw, sh, -1));

        // Row 5 (Main): 7 (CONST), 8 (CONV), 9 (CLR), DEL (INS), AC (OFF)
        double yMain1 = 73.0, mw = 21.8, mh = 13.6;
        keys.add(new KeyDef("7", "7", "CONST", "", KeyCategory.MAIN_GREY, mainCols[0], yMain1, mw, mh, -1));
        keys.add(new KeyDef("8", "8", "CONV", "", KeyCategory.MAIN_GREY, mainCols[1], yMain1, mw, mh, -1));
        keys.add(new KeyDef("9", "9", "CLR", "", KeyCategory.MAIN_GREY, mainCols[2], yMain1, mw, mh, -1));
        keys.add(new KeyDef("DEL", "DEL", "INS", "", KeyCategory.MAIN_GREEN, mainCols[3], yMain1, mw, mh, -1));
        keys.add(new KeyDef("AC", "AC", "OFF", "", KeyCategory.MAIN_GREEN, mainCols[4], yMain1, mw, mh, -1));

        // Row 6 (Main): 4 (MATRIX), 5 (VECTOR), 6, × (nPr), ÷ (nCr)
        double yMain2 = 96.0;
        keys.add(new KeyDef("4", "4", "MATRIX", "", KeyCategory.MAIN_GREY, mainCols[0], yMain2, mw, mh, -1));
        keys.add(new KeyDef("5", "5", "VECTOR", "", KeyCategory.MAIN_GREY, mainCols[1], yMain2, mw, mh, -1));
        keys.add(new KeyDef("6", "6", "", "", KeyCategory.MAIN_GREY, mainCols[2], yMain2, mw, mh, -1));
        keys.add(new KeyDef("MUL", "×", "nPr", "", KeyCategory.MAIN_GREY, mainCols[3], yMain2, mw, mh, -1));
        keys.add(new KeyDef("DIV", "÷", "nCr", "", KeyCategory.MAIN_GREY, mainCols[4], yMain2, mw, mh, -1));

        // Row 7 (Main): 1 (STAT), 2 (CMPLX), 3 (BASE), + (Pol), - (Rec)
        double yMain3 = 119.0;
        keys.add(new KeyDef("1", "1", "STAT", "", KeyCategory.MAIN_GREY, mainCols[0], yMain3, mw, mh, -1));
        keys.add(new KeyDef("2", "2", "CMPLX", "", KeyCategory.MAIN_GREY, mainCols[1], yMain3, mw, mh, -1));
        keys.add(new KeyDef("3", "3", "BASE", "", KeyCategory.MAIN_GREY, mainCols[2], yMain3, mw, mh, -1));
        keys.add(new KeyDef("ADD", "+", "Pol", "", KeyCategory.MAIN_GREY, mainCols[3], yMain3, mw, mh, -1));
        keys.add(new KeyDef("SUB", "−", "Rec", "", KeyCategory.MAIN_GREY, mainCols[4], yMain3, mw, mh, -1));

        // Row 8 (Main): 0 (Rnd), . (Ran#), ×10ˣ (π | e), Ans (DRG), =
        double yMain4 = 142.0;
        keys.add(new KeyDef("0", "0", "Rnd", "", KeyCategory.MAIN_GREY, mainCols[0], yMain4, mw, mh, -1));
        keys.add(new KeyDef("DOT", ".", "Ran#", "", KeyCategory.MAIN_GREY, mainCols[1], yMain4, mw, mh, -1));
        keys.add(new KeyDef("EXP", "×10ˣ", "π", "e", KeyCategory.MAIN_GREY, mainCols[2], yMain4, mw, mh, -1));
        keys.add(new KeyDef("ANS", "Ans", "DRG", "", KeyCategory.MAIN_GREY, mainCols[3], yMain4, mw, mh, -1));
        keys.add(new KeyDef("EQ", "=", "", "", KeyCategory.MAIN_GREY, mainCols[4], yMain4, mw, mh, -1));

        return keys;
    }
}