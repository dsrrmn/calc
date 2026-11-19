import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * Generates clean, alias-free high-resolution textures and PhongMaterials
 * for the faceplate, back cover, solar cell, metallic buttons, and 3D keycaps.
 */
public final class TextureFactory {

    private static final int FACE_W = 1560;
    private static final int FACE_H = 3200;

    private TextureFactory() {}

    public static WritableImage createFaceplateTexture(List<KeyDef> keys) {
        Canvas canvas = new Canvas(FACE_W, FACE_H);
        GraphicsContext g = canvas.getGraphicsContext2D();

        // Smooth satin dark charcoal-black matte plastic finish (#1b1e23) — zero grainy noise
        LinearGradient bodyGrad = new LinearGradient(0, 0, FACE_W, FACE_H, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#20242a")),
                new Stop(0.45, Color.web("#1b1e23")),
                new Stop(1.0, Color.web("#16191e")));
        g.setFill(bodyGrad);
        g.fillRect(0, 0, FACE_W, FACE_H);

        // Perimeter bezel lip highlight & recessed faceplate inner shadow
        g.setStroke(Color.rgb(54, 60, 70, 0.85));
        g.setLineWidth(18.0);
        g.strokeRoundRect(28, 28, FACE_W - 56, FACE_H - 56, 260, 260);

        g.setStroke(Color.rgb(8, 10, 13, 0.90));
        g.setLineWidth(14.0);
        g.strokeRoundRect(56, 56, FACE_W - 112, FACE_H - 112, 220, 220);

        // Engraved mold seam around Main Keypad zone
        g.setStroke(Color.rgb(11, 13, 17, 0.80));
        g.setLineWidth(5.0);
        g.strokeRoundRect(88, 2206, FACE_W - 176, 890, 42, 42);
        g.setStroke(Color.rgb(38, 43, 52, 0.65));
        g.setLineWidth(2.5);
        g.strokeRoundRect(92, 2210, FACE_W - 184, 882, 40, 40);

        // Soft Ambient Occlusion around LCD Bezel, Solar Frame & Central D-Pad Recess
        g.setFill(Color.rgb(7, 8, 11, 0.72));
        g.fillRoundRect(mmToPxX(-65.5), mmToPxY(-117.5), mmToPxW(131.0), mmToPxW(71.0), 75, 75);
        g.fillRoundRect(mmToPxX(9.0), mmToPxY(-147.2), mmToPxW(55.0), mmToPxW(21.5), 40, 40);

        RadialGradient dpadAO = new RadialGradient(0, 0, mmToPxX(0), mmToPxY(-23.0), mmToPxW(21.0),
                false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(7, 9, 12, 0.88)),
                new Stop(0.84, Color.rgb(10, 12, 15, 0.68)),
                new Stop(1.0, Color.rgb(27, 30, 35, 0.0)));
        g.setFill(dpadAO);
        g.fillOval(mmToPxX(-21.0), mmToPxY(-44.0), mmToPxW(42.0), mmToPxW(42.0));

        // Soft keywell shadow halos
        for (KeyDef kd : keys) {
            if (kd.category == KeyDef.KeyCategory.DPAD_ROCKER) continue;
            double kx = mmToPxX(kd.x - kd.w * 0.5) - 16;
            double ky = mmToPxY(kd.y - kd.h * 0.5) - 14;
            double kw = mmToPxW(kd.w) + 32;
            double kh = mmToPxW(kd.h) + 32;
            g.setFill(Color.rgb(7, 8, 11, 0.58));
            if (kd.category == KeyDef.KeyCategory.SILVER_ROUND) {
                g.fillOval(kx, ky, kw, kh);
            } else {
                g.fillRoundRect(kx, ky, kw, kh, 44, 44);
            }
        }

        // Crisp White Silkscreened Branding Header on Upper-Left
        g.setFill(Color.web("#f0f3f8"));
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font("Arial Black", FontWeight.EXTRA_BOLD, 66));
        g.fillText("CASIO", mmToPxX(-63.0), mmToPxY(-139.0));

        g.setFont(Font.font("Arial", FontWeight.BOLD, 37));
        g.fillText("fx-991ES PLUS", mmToPxX(-63.0), mmToPxY(-129.0));

        g.setFill(Color.web("#d5dce6"));
        g.setFont(Font.font("Times New Roman", FontWeight.BOLD, FontPosture.ITALIC, 30));
        g.fillText("NATURAL-V.P.A.M.", mmToPxX(-63.0), mmToPxY(-120.2));

        // Right-aligned "2nd edition" above the LCD screen
        g.setFill(Color.web("#dce2ec"));
        g.setTextAlign(TextAlignment.RIGHT);
        g.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        g.fillText("2nd edition", mmToPxX(62.5), mmToPxY(-120.2));

        // Secondary Silkscreen Labels Above Every Key
        Color goldShift = Color.web("#d8a948");
        Color pinkAlpha = Color.web("#d95b76");
        Color whiteSilk = Color.web("#e8ecf2");

        for (KeyDef kd : keys) {
            double cx = mmToPxX(kd.x);
            double topY = mmToPxY(kd.y - kd.h * 0.5) - 18.0;

            if (kd.category == KeyDef.KeyCategory.SILVER_ROUND) {
                topY = mmToPxY(kd.y - kd.h * 0.5) - 22.0;
                g.setFont(Font.font("Arial", FontWeight.BOLD, 28));
                switch (kd.id) {
                    case "SHIFT" -> {
                        g.setFill(goldShift);
                        g.setTextAlign(TextAlignment.CENTER);
                        g.fillText("SHIFT", cx, topY);
                    }
                    case "ALPHA" -> {
                        g.setFill(pinkAlpha);
                        g.setTextAlign(TextAlignment.CENTER);
                        g.fillText("ALPHA", cx, topY);
                    }
                    case "MODE" -> {
                        g.setFill(whiteSilk);
                        g.setTextAlign(TextAlignment.RIGHT);
                        g.fillText("MODE", cx - 4, topY);
                        g.setFill(goldShift);
                        g.setTextAlign(TextAlignment.LEFT);
                        g.setFont(Font.font("Arial", FontWeight.BOLD, 24));
                        g.fillText("SETUP", cx + 6, topY);
                    }
                    case "ON" -> {
                        g.setFill(whiteSilk);
                        g.setTextAlign(TextAlignment.CENTER);
                        g.fillText("ON", cx, topY);
                    }
                }
                continue;
            }

            if (kd.category == KeyDef.KeyCategory.DPAD_ROCKER) continue;

            boolean hasShift = kd.shiftLabel != null && !kd.shiftLabel.isEmpty();
            boolean hasAlpha = kd.alphaLabel != null && !kd.alphaLabel.isEmpty();

            g.setFont(Font.font("Arial", FontWeight.BOLD,
                    kd.category == KeyDef.KeyCategory.SCI_PILL ? 26.5 : 28.0));

            if (hasShift && hasAlpha) {
                double halfSpan = mmToPxW(kd.w) * 0.48;
                g.setFill(goldShift);
                g.setTextAlign(TextAlignment.LEFT);
                g.fillText(kd.shiftLabel, cx - halfSpan, topY);

                g.setFill(pinkAlpha);
                g.setTextAlign(TextAlignment.RIGHT);
                g.fillText(kd.alphaLabel, cx + halfSpan, topY);
            } else if (hasShift) {
                g.setFill(goldShift);
                g.setTextAlign(TextAlignment.CENTER);
                g.fillText(kd.shiftLabel, cx, topY);
            } else if (hasAlpha) {
                g.setFill(pinkAlpha);
                g.setTextAlign(TextAlignment.CENTER);
                g.fillText(kd.alphaLabel, cx, topY);
            }
        }

        WritableImage finalImg = new WritableImage(FACE_W, FACE_H);
        canvas.snapshot(null, finalImg);
        return finalImg;
    }

    public static WritableImage createBackCoverTexture() {
        int w = 1024, h = 2048;
        Canvas canvas = new Canvas(w, h);
        GraphicsContext g = canvas.getGraphicsContext2D();

        LinearGradient bg = new LinearGradient(0, 0, w, h, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#1f2329")),
                new Stop(0.5, Color.web("#1b1e23")),
                new Stop(1.0, Color.web("#15181c")));
        g.setFill(bg);
        g.fillRect(0, 0, w, h);

        // Upper Battery Compartment Seam Line
        double seamY = h * 0.44;
        g.setStroke(Color.rgb(7, 8, 11));
        g.setLineWidth(6.0);
        g.strokeLine(70, seamY, w - 70, seamY);
        g.setStroke(Color.rgb(42, 46, 54));
        g.setLineWidth(2.5);
        g.strokeLine(70, seamY + 5, w - 70, seamY + 5);

        // Recessed circular screw well shadows
        double[][] screwUVs = {
                {0.122, 0.078}, {0.878, 0.078},
                {0.109, 0.444}, {0.891, 0.444},
                {0.122, 0.919}, {0.878, 0.919}
        };
        for (double[] uv : screwUVs) {
            double sx = uv[0] * w, sy = uv[1] * h;
            g.setFill(Color.rgb(8, 9, 12));
            g.fillOval(sx - 24, sy - 24, 48, 48);
            g.setStroke(Color.rgb(40, 44, 52));
            g.setLineWidth(3.0);
            g.strokeOval(sx - 25, sy - 25, 50, 50);
        }

        // Molded Manufacturer Specification Plate
        double plateX = w * 0.18, plateY = h * 0.52, plateW = w * 0.64, plateH = h * 0.25;
        g.setFill(Color.rgb(22, 25, 30));
        g.fillRoundRect(plateX, plateY, plateW, plateH, 24, 24);
        g.setStroke(Color.rgb(10, 12, 15));
        g.setLineWidth(4.0);
        g.strokeRoundRect(plateX, plateY, plateW, plateH, 24, 24);

        g.setFill(Color.rgb(122, 130, 142));
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font("Arial", FontWeight.BOLD, 34));
        g.fillText("CASIO", w * 0.5, plateY + 68);
        g.setFont(Font.font("Arial", FontWeight.BOLD, 25));
        g.fillText("fx-991ES PLUS  2nd edition", w * 0.5, plateY + 115);

        g.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
        g.fillText("RATING : DC 1.5V  0.0002W", w * 0.5, plateY + 175);
        g.fillText("USE BATTERY LR44 × 1", w * 0.5, plateY + 212);
        g.fillText("CASIO COMPUTER CO., LTD.", w * 0.5, plateY + 268);
        g.fillText("MADE IN THAILAND", w * 0.5, plateY + 305);

        WritableImage img = new WritableImage(w, h);
        canvas.snapshot(null, img);
        return img;
    }

    public static PhongMaterial createSolarPanelMaterial() {
        int w = 512, h = 180;
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();

        LinearGradient bg = new LinearGradient(0, 0, w, h, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#3b2824")),
                new Stop(0.48, Color.web("#4a322c")),
                new Stop(0.54, Color.web("#34211d")),
                new Stop(1.0, Color.web("#2a1b18")));
        g.setFill(bg);
        g.fillRect(0, 0, w, h);

        // 4 photovoltaic sub-cell vertical busbars
        g.setStroke(Color.rgb(170, 132, 102, 0.62));
        g.setLineWidth(3.5);
        for (int i = 1; i <= 3; i++) {
            double x = (w / 4.0) * i;
            g.strokeLine(x, 8, x, h - 8);
        }

        g.setStroke(Color.rgb(8, 6, 6, 0.80));
        g.setLineWidth(14.0);
        g.strokeRect(0, 0, w, h);

        WritableImage img = new WritableImage(w, h);
        c.snapshot(null, img);

        PhongMaterial mat = new PhongMaterial(Color.WHITE);
        mat.setDiffuseMap(img);
        mat.setSpecularColor(Color.rgb(145, 140, 135));
        mat.setSpecularPower(74.0);
        return mat;
    }

    public static PhongMaterial createBrushedSilverMaterial() {
        int sz = 256;
        Canvas c = new Canvas(sz, sz);
        PixelWriter pw = c.getGraphicsContext2D().getPixelWriter();
        double cx = sz * 0.5, cy = sz * 0.5;

        for (int y = 0; y < sz; y++) {
            for (int x = 0; x < sz; x++) {
                double dx = x - cx, dy = y - cy;
                double r = Math.hypot(dx, dy) / (sz * 0.5);
                double ang = Math.atan2(dy, dx);
                double aniso = Math.cos(2.0 * (ang + 0.65)) * 16.0;
                double bevel = (r > 0.80) ? -32.0 * ((r - 0.80) / 0.20) : 0.0;
                int base = (int) Math.max(80, Math.min(230, 172.0 + aniso + bevel));
                int bBlue = Math.min(242, base + 7);
                pw.setArgb(x, y, (255 << 24) | (base << 16) | ((base + 2) << 8) | bBlue);
            }
        }

        WritableImage img = new WritableImage(sz, sz);
        c.snapshot(null, img);

        PhongMaterial mat = new PhongMaterial(Color.WHITE);
        mat.setDiffuseMap(img);
        mat.setSpecularColor(Color.rgb(160, 168, 178));
        mat.setSpecularPower(46.0);
        return mat;
    }

    public static PhongMaterial createDpadRockerMaterial(int dir) {
        int sz = 256;
        Canvas c = new Canvas(sz, sz);
        GraphicsContext g = c.getGraphicsContext2D();

        LinearGradient lg = new LinearGradient(0, 0, 0, sz, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#bcc3ce")),
                new Stop(0.45, Color.web("#a8b0bc")),
                new Stop(1.0, Color.web("#87909c")));
        g.setFill(lg);
        g.fillRect(0, 0, sz, sz);

        double cx = sz * 0.5, cy = sz * 0.36;
        g.setFill(Color.rgb(225, 232, 240, 0.65));
        g.fillPolygon(
                new double[]{cx, cx - 25, cx + 25},
                new double[]{cy - 20, cy + 18, cy + 18},
                3
        );
        g.setFill(Color.web("#323842"));
        g.fillPolygon(
                new double[]{cx, cx - 24, cx + 24},
                new double[]{cy - 22, cy + 16, cy + 16},
                3
        );

        if (dir == 0) {
            g.setFill(Color.web("#363d48"));
            g.setFont(Font.font("Arial", FontWeight.BOLD, 25));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText("REPLAY", cx, sz * 0.80);
        }

        WritableImage img = new WritableImage(sz, sz);
        c.snapshot(null, img);

        PhongMaterial mat = new PhongMaterial(Color.WHITE);
        mat.setDiffuseMap(img);
        mat.setSpecularColor(Color.rgb(155, 162, 174));
        mat.setSpecularPower(42.0);
        return mat;
    }

    public static PhongMaterial createKeycapMaterial(KeyDef kd, Color baseColor, Color specColor) {
        int w = 280, h = 192;
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();

        Color rimColor = baseColor.interpolate(Color.BLACK, 0.28);
        Color crownColor = baseColor.interpolate(Color.WHITE, 0.06);
        RadialGradient rg = new RadialGradient(0, 0, w * 0.5, h * 0.45, w * 0.62,
                false, CycleMethod.NO_CYCLE,
                new Stop(0.0, crownColor),
                new Stop(0.75, baseColor),
                new Stop(1.0, rimColor));
        g.setFill(rg);
        g.fillRect(0, 0, w, h);

        // Subtle inner bevel highlight rim around top face of the keycap
        g.setStroke(Color.rgb(255, 255, 255, 0.09));
        g.setLineWidth(3.5);
        g.strokeRoundRect(22, 18, w - 44, h - 36, 36, 36);

        g.setFill(Color.web("#f4f7fb"));
        g.setTextAlign(TextAlignment.CENTER);

        boolean isSci = kd.category == KeyDef.KeyCategory.SCI_PILL;
        String label = kd.primaryLabel;

        if (isSci) {
            if ("x■".equals(label)) {
                g.setFont(Font.font("Times New Roman", FontWeight.BOLD, FontPosture.ITALIC, 62));
                g.fillText("x", w * 0.43, h * 0.64);
                g.fillRect(w * 0.52, h * 0.30, 24, 24);
            } else if ("log□".equals(label)) {
                g.setFont(Font.font("Arial", FontWeight.BOLD, 46));
                g.fillText("log", w * 0.38, h * 0.58);
                g.setStroke(Color.web("#f4f7fb"));
                g.setLineWidth(3.5);
                g.strokeRect(w * 0.58, h * 0.52, 18, 18);
                g.fillRect(w * 0.70, h * 0.34, 20, 22);
            } else if ("√".equals(label)) {
                g.setFont(Font.font("Arial", FontWeight.BOLD, 56));
                g.fillText("√", w * 0.41, h * 0.66);
                g.fillRect(w * 0.51, h * 0.38, 26, 26);
            } else {
                int fontSize = label.length() >= 4 ? 44 : 54;
                g.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));
                g.fillText(label, w * 0.5, h * 0.62);
            }
        } else {
            int fontSize = switch (label.length()) {
                case 1 -> 78;
                case 2 -> 62;
                case 3 -> 54;
                default -> 46;
            };
            g.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));
            g.fillText(label, w * 0.5, h * 0.64);
        }

        WritableImage img = new WritableImage(w, h);
        c.snapshot(null, img);

        PhongMaterial mat = new PhongMaterial(Color.WHITE);
        mat.setDiffuseMap(img);
        mat.setSpecularColor(specColor);
        mat.setSpecularPower(26.0);
        return mat;
    }

    public static PhongMaterial createScrewMaterial() {
        int sz = 128;
        Canvas c = new Canvas(sz, sz);
        GraphicsContext g = c.getGraphicsContext2D();

        g.setFill(Color.web("#767d88"));
        g.fillRect(0, 0, sz, sz);
        g.setFill(Color.web("#25282e"));
        g.fillRoundRect(sz * 0.22, sz * 0.43, sz * 0.56, sz * 0.14, 6, 6);
        g.fillRoundRect(sz * 0.43, sz * 0.22, sz * 0.14, sz * 0.56, 6, 6);

        WritableImage img = new WritableImage(sz, sz);
        c.snapshot(null, img);

        PhongMaterial mat = new PhongMaterial(Color.WHITE);
        mat.setDiffuseMap(img);
        mat.setSpecularColor(Color.rgb(160, 165, 175));
        mat.setSpecularPower(40.0);
        return mat;
    }

    private static double mmToPxX(double mmX) {
        return ((mmX + ProceduralMeshFactory.BODY_W * 0.5) / ProceduralMeshFactory.BODY_W) * FACE_W;
    }

    private static double mmToPxY(double mmY) {
        return ((mmY + ProceduralMeshFactory.BODY_H * 0.5) / ProceduralMeshFactory.BODY_H) * FACE_H;
    }

    private static double mmToPxW(double mmW) {
        return (mmW / ProceduralMeshFactory.BODY_W) * FACE_W;
    }
}