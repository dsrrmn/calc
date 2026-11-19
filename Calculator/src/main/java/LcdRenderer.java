import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentic Casio LCD Renderer combining:
 *  - Fixed top status indicator segments ([S], [A], STO, RCL, [M], [D]/[R]/[G], Math)
 *  - 5x7 Dot-Matrix Pixel Grid for the upper expression line & menus
 *  - Traditional Casio Solid Brown Box Cursor (#4a3222)
 *  - True 7-Segment / 14-Segment geometric LCD display for the lower result readout
 */
public final class LcdRenderer {

    public static final int LCD_W = 1024;
    public static final int LCD_H = 490;

    // Authentic STN LCD Colors
    private static final Color INK_ON = Color.web("#182219");
    private static final Color INK_GHOST = Color.rgb(24, 34, 25, 0.065);
    private static final Color CURSOR_BROWN = Color.web("#4a3222");

    // 5x7 Dot-Matrix Bitmap Font ROM (7 rows, each 5-bit integer from MSB bit 4 to LSB bit 0)
    private static final Map<Character, int[]> DOT_FONT = new HashMap<>();

    static {
        initDotMatrixRom();
    }

    private LcdRenderer() {}

    public static void render(Canvas canvas, WritableImage targetImage,
                              CalculatorEngine engine, boolean cursorBlinkOn) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = LCD_W;
        double h = LCD_H;

        // 1. Pale sage-green reflective STN LCD background (#d4dfd0)
        RadialGradient lcdBg = new RadialGradient(0, 0, w * 0.5, h * 0.5, w * 0.68,
                false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#d6e1d2")),
                new Stop(0.80, Color.web("#ced9ca")),
                new Stop(1.0, Color.web("#bfcabc")));
        g.setFill(lcdBg);
        g.fillRect(0, 0, w, h);

        // Recessed bezel inner drop shadows
        drawRecessedBezelShadows(g, w, h);

        if (engine.isPoweredOff()) {
            canvas.snapshot(null, targetImage);
            return;
        }

        // 2. Top Fixed LCD Status Bar Segments
        drawStatusBadge(g, 36, 18, "S", engine.isShiftActive());
        drawStatusBadge(g, 74, 18, "A", engine.isAlphaActive());
        drawStatusText(g, 118, 37, "STO", engine.isStoMode());
        drawStatusText(g, 178, 37, "RCL", engine.isRclMode());
        drawStatusBadge(g, 245, 18, "M", Math.abs(engine.getMemoryM()) > 1e-15);

        drawStatusBadge(g, 360, 18, "D", engine.getAngleMode() == CalculatorEngine.AngleMode.DEG);
        drawStatusBadge(g, 396, 18, "R", engine.getAngleMode() == CalculatorEngine.AngleMode.RAD);
        drawStatusBadge(g, 432, 18, "G", engine.getAngleMode() == CalculatorEngine.AngleMode.GRA);

        drawStatusText(g, 505, 37, engine.getSystemModeBadge(), !engine.getSystemModeBadge().isEmpty());
        drawStatusText(g, 610, 37, "FIX", engine.getFixDigits() >= 0);
        drawStatusText(g, 675, 37, "SCI", engine.getSciDigits() >= 0);
        drawStatusText(g, 780, 37, "▲▼", engine.hasHistory());
        drawStatusText(g, 895, 37, "Math", true);

        // 3. Check if an interactive modal menu is active (rendered on 4-row 5x7 dot-matrix)
        List<String> menuLines = engine.getActiveMenuLines();
        if (menuLines != null && !menuLines.isEmpty()) {
            double my = 68;
            for (String line : menuLines) {
                drawDotMatrixLine(g, 38, my, line, -1, false, false, 20, 6.2, 1.3);
                my += 98;
            }
            canvas.snapshot(null, targetImage);
            return;
        }

        // 4. Upper Expression Line: 5x7 Dot-Matrix Grid + Traditional Brown Box Cursor
        String exprLeft = engine.getExpressionBeforeCursor();
        String exprRight = engine.getExpressionAfterCursor();
        String fullExpr = exprLeft + exprRight;
        int cursorPos = exprLeft.length();

        int maxCols = 21;
        int winStart = 0;
        if (cursorPos >= maxCols) {
            winStart = cursorPos - maxCols + 2;
        }
        String visibleExpr = fullExpr.substring(
                Math.min(winStart, fullExpr.length()),
                Math.min(fullExpr.length(), winStart + maxCols));
        int visibleCursorCol = cursorPos - winStart;

        boolean showCursorAtSlot = !engine.isJustEvaluated();
        drawDotMatrixLine(g, 38, 74, visibleExpr,
                showCursorAtSlot ? visibleCursorCol : -1,
                cursorBlinkOn, engine.isInsertMode(),
                maxCols, 6.8, 1.4);

        // 5. Lower Result Readout: True 7-Segment / 14-Segment Display
        String errorMsg = engine.getErrorMessage();
        if (errorMsg != null) {
            drawDotMatrixLine(g, 38, 195, errorMsg, -1, false, false, 21, 6.8, 1.4);
            drawDotMatrixLine(g, 38, 295, "[AC]  :Cancel", -1, false, false, 21, 5.8, 1.3);
            drawDotMatrixLine(g, 38, 375, "[◀][▶]:Goto", -1, false, false, 21, 5.8, 1.3);
        } else {
            // Always render the unlit ghost 7-segment background across the bottom row
            drawGhost7SegmentRow(g);

            if (engine.hasResult()) {
                CalculatorEngine.FormattedResult res = engine.getFormattedResult();
                if (res.isFraction) {
                    draw7SegmentFractionResult(g, res);
                } else {
                    draw7SegmentStandardResult(g, res);
                }
            }
        }

        canvas.snapshot(null, targetImage);
    }

    // =========================================================================================
    // DOT-MATRIX RENDERER & TRADITIONAL BROWN BOX CURSOR
    // =========================================================================================

    private static void drawDotMatrixLine(GraphicsContext g, double startX, double startY,
                                          String text, int cursorCol, boolean cursorBlinkOn,
                                          boolean insertMode, int totalCols,
                                          double dotSize, double dotGap) {
        double pitch = dotSize + dotGap;
        double charStep = 6 * pitch;

        for (int col = 0; col < totalCols; col++) {
            double cx = startX + col * charStep;
            char ch = (col < text.length()) ? text.charAt(col) : ' ';
            int[] glyph = getGlyph(ch);

            boolean isCursorCell = (col == cursorCol) && cursorBlinkOn;

            for (int r = 0; r < 7; r++) {
                int rowBits = glyph[r];
                for (int c = 0; c < 5; c++) {
                    double px = cx + c * pitch;
                    double py = startY + r * pitch;
                    boolean charPixelOn = ((rowBits >> (4 - c)) & 1) == 1;

                    if (isCursorCell) {
                        // Traditional Casio Brown Box Cursor (#4a3222):
                        // Solid 5x7 brown block in normal mode; hollow brown box frame in INS mode
                        boolean cursorPixelOn = !insertMode
                                || (r == 0 || r == 6 || c == 0 || c == 4);
                        if (cursorPixelOn) {
                            g.setFill(CURSOR_BROWN);
                        } else if (charPixelOn) {
                            g.setFill(INK_ON);
                        } else {
                            g.setFill(INK_GHOST);
                        }
                    } else {
                        g.setFill(charPixelOn ? INK_ON : INK_GHOST);
                    }
                    g.fillRect(px, py, dotSize, dotSize);
                }
            }
        }
    }

    // =========================================================================================
    // 7-SEGMENT / 14-SEGMENT GEOMETRIC RESULT RENDERER
    // =========================================================================================

    // 10 main digit slots + 2 superscript exponent slots
    private static final int MAIN_SLOTS = 12;
    private static final double SEG_START_X = 62.0;
    private static final double SEG_BASE_Y = 305.0;
    private static final double DIGIT_W = 46.0;
    private static final double DIGIT_H = 104.0;
    private static final double DIGIT_PITCH = 66.0;
    private static final double SEG_THICK = 9.2;

    private static void drawGhost7SegmentRow(GraphicsContext g) {
        for (int i = 0; i < MAIN_SLOTS; i++) {
            double dx = SEG_START_X + i * DIGIT_PITCH;
            draw14SegmentChar(g, dx, SEG_BASE_Y, DIGIT_W, DIGIT_H, SEG_THICK, (char) 0, false, true);
        }
        // Ghost x10 and 2-digit exponent on far right
        double expX = SEG_START_X + MAIN_SLOTS * DIGIT_PITCH + 18;
        drawStatusText(g, expX - 10, SEG_BASE_Y + DIGIT_H - 6, "×10", false);
        for (int e = 0; e < 3; e++) {
            draw14SegmentChar(g, expX + 42 + e * 32, SEG_BASE_Y - 12, 22, 48, 4.8, (char) 0, false, true);
        }
    }

    private static void draw7SegmentStandardResult(GraphicsContext g, CalculatorEngine.FormattedResult res) {
        String raw = res.mainPart != null ? res.mainPart : "0";

        // Parse characters and attach decimal points to their preceding character slot
        char[] slotChars = new char[MAIN_SLOTS];
        boolean[] slotDots = new boolean[MAIN_SLOTS];
        for (int i = 0; i < MAIN_SLOTS; i++) slotChars[i] = ' ';

        List<Character> chars = new java.util.ArrayList<>();
        List<Boolean> dots = new java.util.ArrayList<>();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '.') {
                if (!dots.isEmpty()) {
                    dots.set(dots.size() - 1, true);
                } else {
                    chars.add('0');
                    dots.add(true);
                }
            } else {
                chars.add(c);
                dots.add(false);
            }
        }

        int count = Math.min(MAIN_SLOTS, chars.size());
        int offset = MAIN_SLOTS - count;
        for (int i = 0; i < count; i++) {
            slotChars[offset + i] = chars.get(i);
            slotDots[offset + i] = dots.get(i);
        }

        for (int i = 0; i < MAIN_SLOTS; i++) {
            if (slotChars[i] != ' ' || slotDots[i]) {
                double dx = SEG_START_X + i * DIGIT_PITCH;
                draw14SegmentChar(g, dx, SEG_BASE_Y, DIGIT_W, DIGIT_H, SEG_THICK,
                        slotChars[i], slotDots[i], false);
            }
        }

        if (res.exponentPart != null && !res.exponentPart.isEmpty()) {
            double expX = SEG_START_X + MAIN_SLOTS * DIGIT_PITCH + 18;
            drawStatusText(g, expX - 10, SEG_BASE_Y + DIGIT_H - 6, "×10", true);
            String expStr = res.exponentPart.replace("+", " ");
            for (int e = 0; e < Math.min(3, expStr.length()); e++) {
                char ec = expStr.charAt(e);
                if (ec != ' ') {
                    draw14SegmentChar(g, expX + 42 + e * 32, SEG_BASE_Y - 12, 22, 48, 4.8,
                            ec, false, false);
                }
            }
        }
    }

    private static void draw7SegmentFractionResult(GraphicsContext g, CalculatorEngine.FormattedResult res) {
        // Render numerator, fraction bar segment, denominator, and optional integer whole part
        // using crisp 7-segment digits on the right side of the result area
        String numStr = String.valueOf(res.fracNum);
        String denStr = String.valueOf(res.fracDen);
        int maxLen = Math.max(numStr.length(), denStr.length());

        double smW = 30.0, smH = 54.0, smPitch = 42.0, smThick = 6.0;
        double rightEdge = SEG_START_X + MAIN_SLOTS * DIGIT_PITCH - 10;
        double barW = Math.max(76.0, maxLen * smPitch + 16.0);
        double barLeft = rightEdge - barW;
        double midY = SEG_BASE_Y + 46.0;

        // Numerator (7-segment digits above bar)
        double numStart = rightEdge - numStr.length() * smPitch;
        for (int i = 0; i < numStr.length(); i++) {
            draw14SegmentChar(g, numStart + i * smPitch, midY - smH - 12,
                    smW, smH, smThick, numStr.charAt(i), false, false);
        }

        // Horizontal LCD fraction bar segment
        g.setFill(INK_ON);
        g.fillRoundRect(barLeft, midY - 3.5, barW, 7.0, 3, 3);

        // Denominator (7-segment digits below bar)
        double denStart = rightEdge - denStr.length() * smPitch;
        for (int i = 0; i < denStr.length(); i++) {
            draw14SegmentChar(g, denStart + i * smPitch, midY + 12,
                    smW, smH, smThick, denStr.charAt(i), false, false);
        }

        // Whole part (for mixed fractions a b/c or minus sign)
        if (res.wholePart != null && !res.wholePart.isEmpty()) {
            String wp = res.wholePart;
            double wpStart = barLeft - wp.length() * DIGIT_PITCH - 14;
            for (int i = 0; i < wp.length(); i++) {
                draw14SegmentChar(g, wpStart + i * DIGIT_PITCH, SEG_BASE_Y - 4,
                        DIGIT_W, DIGIT_H, SEG_THICK, wp.charAt(i), false, false);
            }
        }
    }

    /**
     * Draws a single 7-segment / 14-segment character cell with mitered hexagonal LCD bars.
     * Bitmask:
     *  0:A (top), 1:B (top-right), 2:C (bottom-right), 3:D (bottom),
     *  4:E (bottom-left), 5:F (top-left), 6:G1 (mid-left), 7:G2 (mid-right),
     *  8:H (top-center vert), 9:I (bottom-center vert)
     */
    private static void draw14SegmentChar(GraphicsContext g, double x, double y,
                                          double w, double h, double t,
                                          char ch, boolean dotOn, boolean ghostOnly) {
        int mask = ghostOnly ? 0 : getSegmentMask(ch);
        double halfH = h * 0.5;
        double slant = w * 0.08; // Authentic 5-degree LCD segment slant

        // A: Top horizontal
        drawHorizSeg(g, x + slant, y, w, t, isSegOn(mask, 0), ghostOnly);
        // D: Bottom horizontal
        drawHorizSeg(g, x, y + h - t, w, t, isSegOn(mask, 3), ghostOnly);
        // G1 & G2: Middle horizontal split
        drawHorizSeg(g, x + slant * 0.5, y + halfH - t * 0.5, w * 0.51, t, isSegOn(mask, 6), ghostOnly);
        drawHorizSeg(g, x + slant * 0.5 + w * 0.49, y + halfH - t * 0.5, w * 0.51, t, isSegOn(mask, 7), ghostOnly);

        // F: Top-left vertical
        drawVertSeg(g, x + slant * 0.5, y + t * 0.4, t, halfH - t * 0.4, slant * 0.5, isSegOn(mask, 5), ghostOnly);
        // B: Top-right vertical
        drawVertSeg(g, x + w - t + slant * 0.5, y + t * 0.4, t, halfH - t * 0.4, slant * 0.5, isSegOn(mask, 1), ghostOnly);
        // E: Bottom-left vertical
        drawVertSeg(g, x, y + halfH + t * 0.2, t, halfH - t * 0.6, slant * 0.5, isSegOn(mask, 4), ghostOnly);
        // C: Bottom-right vertical
        drawVertSeg(g, x + w - t, y + halfH + t * 0.2, t, halfH - t * 0.6, slant * 0.5, isSegOn(mask, 2), ghostOnly);

        // Center vertical segments (for '=', 'T', 'I', '+', °/quotes)
        if (!ghostOnly && isSegOn(mask, 8)) {
            drawVertSeg(g, x + w * 0.5 - t * 0.5 + slant * 0.5, y + t, t, halfH - t, slant * 0.3, true, false);
        }
        if (!ghostOnly && isSegOn(mask, 9)) {
            drawVertSeg(g, x + w * 0.5 - t * 0.5, y + halfH + t * 0.5, t, halfH - t * 1.2, slant * 0.3, true, false);
        }

        // Decimal point segment
        if (ghostOnly || dotOn) {
            g.setFill(ghostOnly ? INK_GHOST : INK_ON);
            g.fillOval(x + w + t * 0.45, y + h - t * 1.15, t * 1.1, t * 1.1);
        }
    }

    private static boolean isSegOn(int mask, int bit) {
        return ((mask >> bit) & 1) == 1;
    }

    private static void drawHorizSeg(GraphicsContext g, double x, double y, double w, double t,
                                     boolean on, boolean ghostOnly) {
        if (!ghostOnly && !on) return;
        g.setFill(ghostOnly ? INK_GHOST : INK_ON);
        double r = t * 0.5;
        double pad = t * 0.35;
        double[] px = {x + pad, x + pad + r, x + w - pad - r, x + w - pad, x + w - pad - r, x + pad + r};
        double[] py = {y + r, y, y, y + r, y + t, y + t};
        g.fillPolygon(px, py, 6);
    }

    private static void drawVertSeg(GraphicsContext g, double x, double y, double t, double h,
                                    double slantTop, boolean on, boolean ghostOnly) {
        if (!ghostOnly && !on) return;
        g.setFill(ghostOnly ? INK_GHOST : INK_ON);
        double r = t * 0.5;
        double[] px = {
                x + r + slantTop, x + t + slantTop, x + t,
                x + r, x, x + slantTop
        };
        double[] py = {
                y, y + r, y + h - r,
                y + h, y + h - r, y + r
        };
        g.fillPolygon(px, py, 6);
    }

    private static int getSegmentMask(char ch) {
        // Bits: 0:A, 1:B, 2:C, 3:D, 4:E, 5:F, 6:G1, 7:G2, 8:TopVert, 9:BotVert
        final int A = 1, B = 2, C = 4, D = 8, E = 16, F = 32, G = 64 | 128, V1 = 256, V2 = 512;
        return switch (Character.toUpperCase(ch)) {
            case '0' -> A | B | C | D | E | F;
            case '1' -> B | C;
            case '2' -> A | B | G | E | D;
            case '3' -> A | B | G | C | D;
            case '4' -> F | G | B | C;
            case '5' -> A | F | G | C | D;
            case '6' -> A | F | G | E | C | D;
            case '7' -> A | B | C;
            case '8' -> A | B | C | D | E | F | G;
            case '9' -> A | B | C | D | F | G;
            case '-', '−' -> G;
            case '+' -> G | V1 | V2;
            case '=' -> G | D;
            case 'A' -> A | B | C | E | F | G;
            case 'B' -> F | E | G | C | D;
            case 'C' -> A | F | E | D;
            case 'D' -> B | C | D | E | G;
            case 'E' -> A | F | G | E | D;
            case 'F' -> A | F | G | E;
            case 'M' -> A | B | C | E | F | V1;
            case 'R' -> E | G;
            case 'X' -> B | C | E | F | G;
            case 'Y' -> B | C | D | F | G;
            case '°', '’', '”' -> A | B | F | G;
            default -> 0;
        };
    }

    // =========================================================================================
    // STATUS BAR HELPERS & DOT-MATRIX ROM INITIALIZATION
    // =========================================================================================

    private static void drawRecessedBezelShadows(GraphicsContext g, double w, double h) {
        LinearGradient topShadow = new LinearGradient(0, 0, 0, 32, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(20, 26, 20, 0.44)),
                new Stop(1, Color.rgb(20, 26, 20, 0.0)));
        g.setFill(topShadow);
        g.fillRect(0, 0, w, 32);

        LinearGradient leftShadow = new LinearGradient(0, 0, 26, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(20, 26, 20, 0.34)),
                new Stop(1, Color.rgb(20, 26, 20, 0.0)));
        g.setFill(leftShadow);
        g.fillRect(0, 0, 26, h);
    }

    private static void drawStatusBadge(GraphicsContext g, double x, double y, String letter, boolean active) {
        g.setFill(active ? INK_ON : INK_GHOST);
        g.fillRoundRect(x, y, 28, 24, 3, 3);
        g.setFill(Color.web("#d4dfd0"));
        g.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(letter, x + 14, y + 18);
    }

    private static void drawStatusText(GraphicsContext g, double x, double y, String text, boolean active) {
        if (text == null || text.isEmpty()) return;
        g.setFill(active ? INK_ON : INK_GHOST);
        g.setFont(Font.font("Arial", FontWeight.BOLD, 19));
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText(text, x, y);
    }

    private static int[] getGlyph(char ch) {
        return DOT_FONT.getOrDefault(ch, DOT_FONT.get(' '));
    }

    private static void putGlyph(char ch, int r0, int r1, int r2, int r3, int r4, int r5, int r6) {
        DOT_FONT.put(ch, new int[]{r0, r1, r2, r3, r4, r5, r6});
    }

    private static void initDotMatrixRom() {
        putGlyph(' ', 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000);
        putGlyph('0', 0b01110, 0b10001, 0b10011, 0b10101, 0b11001, 0b10001, 0b01110);
        putGlyph('1', 0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110);
        putGlyph('2', 0b01110, 0b10001, 0b00001, 0b00110, 0b01000, 0b10000, 0b11111);
        putGlyph('3', 0b11111, 0b00010, 0b00100, 0b00110, 0b00001, 0b10001, 0b01110);
        putGlyph('4', 0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010);
        putGlyph('5', 0b11111, 0b10000, 0b11110, 0b00001, 0b00001, 0b10001, 0b01110);
        putGlyph('6', 0b00110, 0b01000, 0b10000, 0b11110, 0b10001, 0b10001, 0b01110);
        putGlyph('7', 0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000);
        putGlyph('8', 0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110);
        putGlyph('9', 0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00010, 0b01100);

        // Uppercase Letters
        putGlyph('A', 0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001);
        putGlyph('B', 0b11110, 0b10001, 0b10001, 0b11110, 0b10001, 0b10001, 0b11110);
        putGlyph('C', 0b01110, 0b10001, 0b10000, 0b10000, 0b10000, 0b10001, 0b01110);
        putGlyph('D', 0b11100, 0b10010, 0b10001, 0b10001, 0b10001, 0b10010, 0b11100);
        putGlyph('E', 0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111);
        putGlyph('F', 0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000);
        putGlyph('G', 0b01110, 0b10001, 0b10000, 0b10111, 0b10001, 0b10001, 0b01110);
        putGlyph('H', 0b10001, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001);
        putGlyph('I', 0b01110, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110);
        putGlyph('L', 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111);
        putGlyph('M', 0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001);
        putGlyph('N', 0b10001, 0b11001, 0b10101, 0b10011, 0b10001, 0b10001, 0b10001);
        putGlyph('O', 0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110);
        putGlyph('P', 0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000);
        putGlyph('Q', 0b01110, 0b10001, 0b10001, 0b10001, 0b10101, 0b10010, 0b01101);
        putGlyph('R', 0b11110, 0b10001, 0b10001, 0b11110, 0b10100, 0b10010, 0b10001);
        putGlyph('S', 0b01111, 0b10000, 0b10000, 0b01110, 0b00001, 0b00001, 0b11110);
        putGlyph('T', 0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100);
        putGlyph('U', 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110);
        putGlyph('V', 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100);
        putGlyph('X', 0b10001, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001, 0b10001);
        putGlyph('Y', 0b10001, 0b10001, 0b01010, 0b00100, 0b00100, 0b00100, 0b00100);

        // Lowercase Letters
        putGlyph('a', 0b00000, 0b00000, 0b01110, 0b00001, 0b01111, 0b10001, 0b01111);
        putGlyph('b', 0b10000, 0b10000, 0b11110, 0b10001, 0b10001, 0b10001, 0b11110);
        putGlyph('c', 0b00000, 0b00000, 0b01110, 0b10000, 0b10000, 0b10001, 0b01110);
        putGlyph('d', 0b00001, 0b00001, 0b01111, 0b10001, 0b10001, 0b10001, 0b01111);
        putGlyph('e', 0b00000, 0b00000, 0b01110, 0b10001, 0b11111, 0b10000, 0b01110);
        putGlyph('f', 0b00110, 0b01001, 0b01000, 0b11100, 0b01000, 0b01000, 0b01000);
        putGlyph('g', 0b00000, 0b01111, 0b10001, 0b10001, 0b01111, 0b00001, 0b01110);
        putGlyph('h', 0b10000, 0b10000, 0b11110, 0b10001, 0b10001, 0b10001, 0b10001);
        putGlyph('i', 0b00100, 0b00000, 0b01100, 0b00100, 0b00100, 0b00100, 0b01110);
        putGlyph('l', 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110);
        putGlyph('m', 0b00000, 0b00000, 0b11010, 0b10101, 0b10101, 0b10001, 0b10001);
        putGlyph('n', 0b00000, 0b00000, 0b11110, 0b10001, 0b10001, 0b10001, 0b10001);
        putGlyph('o', 0b00000, 0b00000, 0b01110, 0b10001, 0b10001, 0b10001, 0b01110);
        putGlyph('p', 0b00000, 0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000);
        putGlyph('r', 0b00000, 0b00000, 0b10110, 0b11001, 0b10000, 0b10000, 0b10000);
        putGlyph('s', 0b00000, 0b00000, 0b01111, 0b10000, 0b01110, 0b00001, 0b11110);
        putGlyph('t', 0b01000, 0b01000, 0b11100, 0b01000, 0b01000, 0b01001, 0b00110);
        putGlyph('u', 0b00000, 0b00000, 0b10001, 0b10001, 0b10001, 0b10011, 0b01101);
        putGlyph('x', 0b00000, 0b00000, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001);
        putGlyph('y', 0b00000, 0b10001, 0b10001, 0b10001, 0b01111, 0b00001, 0b01110);

        // Operators, Superscripts & Scientific Symbols
        putGlyph('+', 0b00000, 0b00100, 0b00100, 0b11111, 0b00100, 0b00100, 0b00000);
        putGlyph('-', 0b00000, 0b00000, 0b00000, 0b11111, 0b00000, 0b00000, 0b00000);
        putGlyph('−', 0b00000, 0b00000, 0b00000, 0b11111, 0b00000, 0b00000, 0b00000);
        putGlyph('×', 0b00000, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001, 0b00000);
        putGlyph('÷', 0b00000, 0b00100, 0b00000, 0b11111, 0b00000, 0b00100, 0b00000);
        putGlyph('/', 0b00001, 0b00010, 0b00010, 0b00100, 0b01000, 0b01000, 0b10000);
        putGlyph('=', 0b00000, 0b00000, 0b11111, 0b00000, 0b11111, 0b00000, 0b00000);
        putGlyph('(', 0b00010, 0b00100, 0b01000, 0b01000, 0b01000, 0b00100, 0b00010);
        putGlyph(')', 0b01000, 0b00100, 0b00010, 0b00010, 0b00010, 0b00100, 0b01000);
        putGlyph('[', 0b01110, 0b01000, 0b01000, 0b01000, 0b01000, 0b01000, 0b01110);
        putGlyph(']', 0b01110, 0b00010, 0b00010, 0b00010, 0b00010, 0b00010, 0b01110);
        putGlyph('.', 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b01100, 0b01100);
        putGlyph(',', 0b00000, 0b00000, 0b00000, 0b00000, 0b00110, 0b00010, 0b00100);
        putGlyph(':', 0b00000, 0b00100, 0b00100, 0b00000, 0b00100, 0b00100, 0b00000);
        putGlyph('^', 0b00100, 0b01010, 0b10001, 0b00000, 0b00000, 0b00000, 0b00000);
        putGlyph('!', 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00000, 0b00100);
        putGlyph('%', 0b11001, 0b11010, 0b00010, 0b00100, 0b01000, 0b01011, 0b10011);
        putGlyph('#', 0b01010, 0b11111, 0b01010, 0b01010, 0b11111, 0b01010, 0b00000);
        putGlyph('√', 0b00011, 0b00100, 0b00100, 0b10100, 0b11000, 0b01000, 0b00000);
        putGlyph('²', 0b01100, 0b10010, 0b00100, 0b01000, 0b11110, 0b00000, 0b00000);
        putGlyph('³', 0b11100, 0b00100, 0b01100, 0b00100, 0b11100, 0b00000, 0b00000);
        putGlyph('ˣ', 0b10100, 0b01000, 0b10100, 0b00000, 0b00000, 0b00000, 0b00000);
        putGlyph('⁻', 0b00000, 0b11100, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000);
        putGlyph('¹', 0b00100, 0b01100, 0b00100, 0b01110, 0b00000, 0b00000, 0b00000);
        putGlyph('₀', 0b00000, 0b00000, 0b00000, 0b01100, 0b10010, 0b10010, 0b01100);
        putGlyph('ₙ', 0b00000, 0b00000, 0b00000, 0b11100, 0b10010, 0b10010, 0b10010);
        putGlyph('π', 0b00000, 0b11111, 0b01010, 0b01010, 0b01010, 0b01010, 0b10001);
        putGlyph('∫', 0b00011, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b11000);
        putGlyph('Σ', 0b11111, 0b01000, 0b00100, 0b00010, 0b00100, 0b01000, 0b11111);
        putGlyph('°', 0b00110, 0b01001, 0b01001, 0b00110, 0b00000, 0b00000, 0b00000);
        putGlyph('⌟', 0b00001, 0b00001, 0b00001, 0b00001, 0b11111, 0b00000, 0b00000);
        putGlyph('∠', 0b00001, 0b00010, 0b00100, 0b01000, 0b10000, 0b11111, 0b00000);
        putGlyph('◀', 0b00010, 0b00110, 0b01110, 0b11110, 0b01110, 0b00110, 0b00010);
        putGlyph('▶', 0b01000, 0b01100, 0b01110, 0b01111, 0b01110, 0b01100, 0b01000);
        putGlyph('~', 0b00000, 0b00000, 0b01000, 0b10101, 0b00010, 0b00000, 0b00000);
    }
}