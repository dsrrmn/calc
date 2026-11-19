package com.calculator.app;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public final class Theme {

    // --- 5-Color Strict Matte Hardware Palette ---
    public static final String HEX_BG = "#000000";
    public static final String HEX_SURFACE = "#2C2B29";
    public static final String HEX_SURFACE_ELEVATED = "#2C2B29";
    public static final String HEX_TERRACOTTA = "#B2633A";
    public static final String HEX_TERRACOTTA_HOVER = "#BD6D44";
    public static final String HEX_RUST = "#7E3C21";
    public static final String HEX_RUST_HOVER = "#8E4426";
    public static final String HEX_STONE = "#8F8E8A";
    public static final String HEX_WHITE = "#FFFFFF";
    public static final String HEX_MUTED = "#8F8E8A";
    public static final String HEX_BORDER = "#3D3B38";
    public static final String HEX_BORDER_SUBTLE = "#242321";

    public static final Color COLOR_BG = Color.web(HEX_BG);
    public static final Color COLOR_SURFACE = Color.web(HEX_SURFACE);
    public static final Color COLOR_SURFACE_ELEVATED = Color.web(HEX_SURFACE_ELEVATED);
    public static final Color COLOR_TERRACOTTA = Color.web(HEX_TERRACOTTA);
    public static final Color COLOR_RUST = Color.web(HEX_RUST);
    public static final Color COLOR_STONE = Color.web(HEX_STONE);
    public static final Color COLOR_WHITE = Color.web(HEX_WHITE);
    public static final Color COLOR_BORDER = Color.web("#3D3B38");

    // --- Tactile Hardware Keycap Color Triplets ---
    // Layer 0: Skirt / Bottom Shadow Edge
    // Layer 1: Top Edge Specular Highlight Rim
    // Layer 2: Main Matte Face Surface
    public static final String CHARCOAL_SKIRT = "#181716";
    public static final String CHARCOAL_HIGHLIGHT = "#3D3B38";
    public static final String CHARCOAL_FACE = "#2C2B29";
    public static final String CHARCOAL_HOVER_FACE = "#383633";
    public static final String CHARCOAL_PRESSED_FACE = "#242321";

    public static final String RUST_SKIRT = "#522513";
    public static final String RUST_HIGHLIGHT = "#994D2D";
    public static final String RUST_FACE = "#7E3C21";
    public static final String RUST_HOVER_FACE = "#8E4426";
    public static final String RUST_PRESSED_FACE = "#6C331C";

    public static final String TERRACOTTA_SKIRT = "#7A4124";
    public static final String TERRACOTTA_HIGHLIGHT = "#C9764B";
    public static final String TERRACOTTA_FACE = "#B2633A";
    public static final String TERRACOTTA_HOVER_FACE = "#BD6D44";
    public static final String TERRACOTTA_PRESSED_FACE = "#9B5430";

    public static final String STONE_SKIRT = "#63625E";
    public static final String STONE_HIGHLIGHT = "#A8A7A3";
    public static final String STONE_FACE = "#8F8E8A";
    public static final String STONE_HOVER_FACE = "#9E9D99";
    public static final String STONE_PRESSED_FACE = "#7A7975";

    // --- 3-Layer CSS Insets for Keycap Depth & Mechanical Travel ---
    public static final String INSETS_NORMAL = "0, 0 0 3 0, 1 0 3 0";
    public static final String INSETS_PRESSED = "0, 2 0 1 0, 3 0 1 0";

    // --- Spatial Rhythm Scale (Strict 4pt / 8pt Scale) ---
    public static final int SPACE_4 = 4;
    public static final int SPACE_8 = 8;
    public static final int SPACE_12 = 12;
    public static final int SPACE_16 = 16;
    public static final int SPACE_20 = 20;
    public static final int SPACE_24 = 24;
    public static final int SPACE_32 = 32;
    public static final int SPACE_48 = 48;
    public static final int SPACE_64 = 64;
    public static final int SPACE_96 = 96;

    // --- Container Widths ---
    public static final int MODAL_WIDTH = 380;
    public static final int CONTAINER_WIDTH = 420;

    // --- Border Radiuses (Braun / Teenage Engineering Compact Radii) ---
    public static final int RADIUS = 6;
    public static final int RADIUS_SM = 4;
    public static final int RADIUS_BASE = 6;
    public static final int RADIUS_MD = 6;
    public static final int RADIUS_LG = 8;

    // --- Typography Stacks ---
    public static final String FONT_FAMILY = "System";
    public static final String FONT_SANS = "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif";
    public static final String FONT_MONO = "'SF Mono', 'Segoe UI Mono', 'Roboto Mono', 'DejaVu Sans Mono', 'Courier New', monospace";

    public static final int TYPE_CAPTION = 12;
    public static final int TYPE_BODY = 14;
    public static final int TYPE_SUBHEAD = 16;
    public static final int TYPE_TITLE = 20;
    public static final int TYPE_DISPLAY = 28;
    public static final int TYPE_HERO = 48;

    public static class KeycapColors {
        public final String skirt;
        public final String highlight;
        public final String face;
        public final String hoverHighlight;
        public final String hoverFace;
        public final String pressedHighlight;
        public final String pressedFace;

        public KeycapColors(String skirt, String highlight, String face,
                            String hoverHighlight, String hoverFace,
                            String pressedHighlight, String pressedFace) {
            this.skirt = skirt;
            this.highlight = highlight;
            this.face = face;
            this.hoverHighlight = hoverHighlight;
            this.hoverFace = hoverFace;
            this.pressedHighlight = pressedHighlight;
            this.pressedFace = pressedFace;
        }
    }

    private Theme() {
    }

    public static KeycapColors getKeycapColors(String bgHex) {
        if (bgHex == null) {
            return new KeycapColors(CHARCOAL_SKIRT, CHARCOAL_HIGHLIGHT, CHARCOAL_FACE,
                    "#45423E", CHARCOAL_HOVER_FACE, "#302F2C", CHARCOAL_PRESSED_FACE);
        }
        if (bgHex.equalsIgnoreCase(HEX_TERRACOTTA) || bgHex.equalsIgnoreCase("#B2633A")) {
            return new KeycapColors(TERRACOTTA_SKIRT, TERRACOTTA_HIGHLIGHT, TERRACOTTA_FACE,
                    "#D48054", TERRACOTTA_HOVER_FACE, TERRACOTTA_HIGHLIGHT, TERRACOTTA_PRESSED_FACE);
        }
        if (bgHex.equalsIgnoreCase(HEX_RUST) || bgHex.equalsIgnoreCase("#7E3C21")) {
            return new KeycapColors(RUST_SKIRT, RUST_HIGHLIGHT, RUST_FACE,
                    "#A8532F", RUST_HOVER_FACE, RUST_HIGHLIGHT, RUST_PRESSED_FACE);
        }
        if (bgHex.equalsIgnoreCase(HEX_STONE) || bgHex.equalsIgnoreCase("#8F8E8A")) {
            return new KeycapColors(STONE_SKIRT, STONE_HIGHLIGHT, STONE_FACE,
                    "#B5B4B0", STONE_HOVER_FACE, STONE_HIGHLIGHT, STONE_PRESSED_FACE);
        }
        return new KeycapColors(CHARCOAL_SKIRT, CHARCOAL_HIGHLIGHT, CHARCOAL_FACE,
                "#45423E", CHARCOAL_HOVER_FACE, "#302F2C", CHARCOAL_PRESSED_FACE);
    }

    public static String buildKeycapStyle(String skirt, String highlight, String face,
                                          String insets, int radius, String textHex,
                                          int fontSize, String fontFam, String weight) {
        return String.format(
                "-fx-background-color: %s, %s, %s; " +
                "-fx-background-insets: %s; " +
                "-fx-background-radius: %dpx; " +
                "-fx-text-fill: %s; " +
                "-fx-font-size: %dpx; " +
                "-fx-font-family: %s; " +
                "-fx-font-weight: %s; " +
                "-fx-border-width: 0;",
                skirt, highlight, face,
                insets,
                radius,
                textHex,
                fontSize,
                fontFam,
                weight
        );
    }

    public static void styleCalcButton(Button button, String bgHex, String textHex, int fontSize) {
        styleCalcButton(button, bgHex, textHex, fontSize, false);
    }

    public static void styleCalcButton(Button button, String bgHex, String textHex, int fontSize, boolean isMono) {
        button.setFocusTraversable(false);
        button.setCursor(Cursor.HAND);

        KeycapColors c = getKeycapColors(bgHex);
        String fontFam = isMono ? FONT_MONO : FONT_SANS;
        String weight = (bgHex.equalsIgnoreCase(HEX_TERRACOTTA) || bgHex.equalsIgnoreCase(HEX_RUST)) ? "bold" : "normal";
        int radius = RADIUS_BASE;

        String baseStyle = buildKeycapStyle(c.skirt, c.highlight, c.face, INSETS_NORMAL, radius, textHex, fontSize, fontFam, weight);
        String hoverStyle = buildKeycapStyle(c.skirt, c.hoverHighlight, c.hoverFace, INSETS_NORMAL, radius, textHex, fontSize, fontFam, weight);
        String pressedStyle = buildKeycapStyle(c.skirt, c.pressedHighlight, c.pressedFace, INSETS_PRESSED, radius, textHex, fontSize, fontFam, weight);

        button.getProperties().put("baseStyle", baseStyle);
        button.getProperties().put("hoverStyle", hoverStyle);
        button.getProperties().put("pressedStyle", pressedStyle);

        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
        button.setOnMousePressed(e -> button.setStyle(pressedStyle));
        button.setOnMouseReleased(e -> button.setStyle(button.isHover() ? hoverStyle : baseStyle));
    }

    public static void triggerKeypressVisual(Button button) {
        if (button == null) return;
        String pressed = (String) button.getProperties().get("pressedStyle");
        String base = (String) button.getProperties().get("baseStyle");
        if (pressed != null && base != null) {
            button.setStyle(pressed);
            PauseTransition pause = new PauseTransition(Duration.millis(90));
            pause.setOnFinished(e -> {
                String target = button.isHover() ? (String) button.getProperties().getOrDefault("hoverStyle", base) : base;
                button.setStyle(target);
            });
            pause.play();
        }
    }

    public static void styleDifficultyButton(Button button) {
        button.setFocusTraversable(false);
        button.setCursor(Cursor.HAND);
        String normalStyle = "-fx-background-color: #2C2B29; -fx-text-fill: #FFFFFF; -fx-background-radius: 6px; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-insets: 0; -fx-border-width: 0;";
        String hoverStyle = "-fx-background-color: #B2633A; -fx-text-fill: #FFFFFF; -fx-background-radius: 6px; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-insets: 0; -fx-border-width: 0;";
        button.setStyle(normalStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
    }

    public static void styleNavButton(Button button, String bgHex) {
        styleNavButton(button, bgHex, null);
    }

    public static void styleNavButton(Button button, String bgHex, String hoverBgHex) {
        button.setFocusTraversable(false);
        button.setCursor(Cursor.HAND);
        button.setPadding(new Insets(SPACE_8, SPACE_16, SPACE_8, SPACE_16));

        String hoverBg = hoverBgHex != null ? hoverBgHex : (bgHex.equalsIgnoreCase(HEX_SURFACE) ? HEX_TERRACOTTA : (bgHex.equalsIgnoreCase(HEX_TERRACOTTA) ? HEX_TERRACOTTA_HOVER : HEX_RUST_HOVER));

        String normalStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-background-radius: 6px; -fx-background-insets: 0; -fx-border-width: 0;",
                bgHex, HEX_WHITE
        );
        String hoverStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-background-radius: 6px; -fx-background-insets: 0; -fx-border-width: 0;",
                hoverBg, HEX_WHITE
        );

        button.setStyle(normalStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
    }

    public static void styleFlatButton(Button button, String bgHex, String textHex, int fontSize, int radius) {
        button.setFocusTraversable(false);
        button.setCursor(Cursor.HAND);

        String hoverBg = bgHex.equalsIgnoreCase(HEX_SURFACE) ? HEX_TERRACOTTA : (bgHex.equalsIgnoreCase(HEX_TERRACOTTA) ? HEX_TERRACOTTA_HOVER : HEX_RUST_HOVER);

        String normalStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: %dpx; " +
                "-fx-font-weight: bold; -fx-background-radius: %dpx; -fx-background-insets: 0; -fx-border-width: 0;",
                bgHex, textHex, fontSize, radius
        );
        String hoverStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: %dpx; " +
                "-fx-font-weight: bold; -fx-background-radius: %dpx; -fx-background-insets: 0; -fx-border-width: 0;",
                hoverBg, textHex, fontSize, radius
        );

        button.setStyle(normalStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
    }

    public static void styleButton(Button button, String bgHex, String hoverBgHex, String textHex, int fontSize) {
        styleFlatButton(button, bgHex, textHex, fontSize, RADIUS_BASE);
    }

    public static void styleStaticButton(Button button, String bgHex, String textHex, int fontSize) {
        button.setFocusTraversable(false);
        KeycapColors c = getKeycapColors(bgHex);
        button.setStyle(buildKeycapStyle(c.skirt, c.highlight, c.face, INSETS_NORMAL, RADIUS_BASE, textHex, fontSize, FONT_SANS, "bold"));
        button.setOnMouseEntered(null);
        button.setOnMouseExited(null);
    }

    public static void styleCard(Region card, int radius, int padding) {
        card.setPadding(new Insets(padding));
        card.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %dpx; " +
                "-fx-border-color: #3D3B38 #242321 #181716 #242321; -fx-border-width: 1; -fx-border-radius: %dpx;",
                HEX_SURFACE, radius, radius
        ));
    }

    public static void styleSurfaceCard(Region region, int padding) {
        styleCard(region, RADIUS_BASE, padding);
    }

    public static Font fontRegular(int size) {
        return Font.font(FONT_FAMILY, FontWeight.NORMAL, size);
    }

    public static Font fontBold(int size) {
        return Font.font(FONT_FAMILY, FontWeight.BOLD, size);
    }

    public static Label createLabel(String text, int fontSize, String hexColor, boolean bold) {
        Label label = new Label(text);
        label.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-size: %dpx; -fx-font-family: %s; -fx-font-weight: %s;",
                hexColor, fontSize, FONT_SANS, bold ? "bold" : "normal"
        ));
        return label;
    }

    public static Region createDivider(String hexColor) {
        Region divider = new Region();
        divider.setMinHeight(1);
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color: " + hexColor + ";");
        return divider;
    }

    public static HBox createKeyValueRow(String keyText, Label valueLabel) {
        HBox row = new HBox(SPACE_8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label keyLabel = createLabel(keyText, TYPE_BODY, HEX_STONE, false);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(keyLabel, spacer, valueLabel);
        return row;
    }
}
