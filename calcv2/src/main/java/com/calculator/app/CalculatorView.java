package com.calculator.app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class CalculatorView extends VBox {

    private static final String KONAMI_CODE = "88224646=+";

    private final Label expressionLabel;
    private final Label resultLabel;
    private final MathEvaluator evaluator;
    private final Runnable onUnlockMania;
    private final StringBuilder konamiBuffer;
    private final Map<String, Button> keyButtonMap;

    private StringBuilder currentExpression;
    private boolean showingResult;
    private boolean showingError;

    public CalculatorView(Runnable onUnlockMania) {
        this.onUnlockMania = onUnlockMania;
        this.evaluator = new MathEvaluator();
        this.konamiBuffer = new StringBuilder();
        this.currentExpression = new StringBuilder();
        this.keyButtonMap = new HashMap<>();
        this.showingResult = false;
        this.showingError = false;

        setSpacing(Theme.SPACE_16);
        setPadding(new Insets(Theme.SPACE_24));
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: " + Theme.HEX_BG + ";");

        // Recessed Display Well (Hardware LCD Cavity Look)
        VBox displayBox = new VBox(Theme.SPACE_8);
        displayBox.setAlignment(Pos.CENTER_RIGHT);
        displayBox.setPadding(new Insets(Theme.SPACE_16, Theme.SPACE_20, Theme.SPACE_16, Theme.SPACE_20));
        displayBox.setMinHeight(100);
        displayBox.setMaxWidth(420);
        displayBox.setStyle(String.format(
                "-fx-background-color: #181716; " +
                "-fx-background-radius: %dpx; " +
                "-fx-border-color: transparent transparent #3A3835 transparent; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: %dpx;",
                Theme.RADIUS_BASE, Theme.RADIUS_BASE
        ));
        displayBox.setEffect(new InnerShadow(8, 0, 3, Color.web("#000000")));

        expressionLabel = new Label("");
        expressionLabel.setMaxWidth(Double.MAX_VALUE);
        expressionLabel.setAlignment(Pos.CENTER_RIGHT);
        expressionLabel.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-size: 13px; -fx-font-family: %s; -fx-font-weight: 500;",
                Theme.HEX_STONE, Theme.FONT_MONO
        ));

        resultLabel = new Label("0");
        resultLabel.setMaxWidth(Double.MAX_VALUE);
        resultLabel.setAlignment(Pos.CENTER_RIGHT);
        resultLabel.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-size: 30px; -fx-font-family: %s; -fx-font-weight: 600;",
                Theme.HEX_WHITE, Theme.FONT_MONO
        ));

        displayBox.getChildren().addAll(expressionLabel, resultLabel);

        GridPane grid = buildKeypadGrid();
        grid.setMaxWidth(420);
        VBox.setVgrow(grid, Priority.ALWAYS);

        getChildren().addAll(displayBox, grid);
    }

    private GridPane buildKeypadGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(Theme.SPACE_8);
        grid.setVgap(Theme.SPACE_8);
        grid.setAlignment(Pos.CENTER);

        for (int c = 0; c < 4; c++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25);
            col.setFillWidth(true);
            grid.getColumnConstraints().add(col);
        }

        for (int r = 0; r < 8; r++) {
            RowConstraints row = new RowConstraints();
            row.setMinHeight(44);
            row.setPrefHeight(52);
            row.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(row);
        }

        String[][] sciRows = {
                {"sin", "cos", "tan", "log"},
                {"ln", "sqrt", "^", "abs"},
                {"(", ")", "pi", "e"}
        };

        for (int r = 0; r < sciRows.length; r++) {
            for (int c = 0; c < 4; c++) {
                String token = sciRows[r][c];
                Button btn = createKeyButton(token, Theme.HEX_SURFACE, Theme.HEX_STONE, 14, false);
                grid.add(btn, c, r);
            }
        }

        // Row 3: AC, CE, %, ÷
        grid.add(createKeyButton("AC", Theme.HEX_TERRACOTTA, Theme.HEX_WHITE, 15, false), 0, 3);
        grid.add(createKeyButton("CE", Theme.HEX_SURFACE_ELEVATED, Theme.HEX_STONE, 15, false), 1, 3);
        grid.add(createKeyButton("%", Theme.HEX_SURFACE, Theme.HEX_STONE, 16, false), 2, 3);
        grid.add(createKeyButton("÷", Theme.HEX_SURFACE, Theme.HEX_STONE, 18, false), 3, 3);

        // Row 4: 7, 8, 9, ×
        grid.add(createKeyButton("7", Theme.HEX_SURFACE, Theme.HEX_WHITE, 18, true), 0, 4);
        grid.add(createKeyButton("8", Theme.HEX_SURFACE, Theme.HEX_WHITE, 18, true), 1, 4);
        grid.add(createKeyButton("9", Theme.HEX_SURFACE, Theme.HEX_WHITE, 18, true), 2, 4);
        grid.add(createKeyButton("×", Theme.HEX_SURFACE, Theme.HEX_STONE, 18, false), 3, 4);

        // Row 5: 4, 5, 6, -
        grid.add(createKeyButton("4", Theme.HEX_SURFACE, Theme.HEX_WHITE, 18, true), 0, 5);
        grid.add(createKeyButton("5", Theme.HEX_SURFACE, Theme.HEX_WHITE, 18, true), 1, 5);
        grid.add(createKeyButton("6", Theme.HEX_SURFACE, Theme.HEX_WHITE, 18, true), 2, 5);
        grid.add(createKeyButton("-", Theme.HEX_SURFACE, Theme.HEX_STONE, 20, false), 3, 5);

        // Row 6: 1, 2, 3, + (spans 2 rows vertically into Row 7)
        grid.add(createKeyButton("1", Theme.HEX_SURFACE, Theme.HEX_WHITE, 18, true), 0, 6);
        grid.add(createKeyButton("2", Theme.HEX_SURFACE, Theme.HEX_WHITE, 18, true), 1, 6);
        grid.add(createKeyButton("3", Theme.HEX_SURFACE, Theme.HEX_WHITE, 18, true), 2, 6);
        Button plusBtn = createKeyButton("+", Theme.HEX_SURFACE_ELEVATED, Theme.HEX_WHITE, 20, false);
        grid.add(plusBtn, 3, 6, 1, 2);

        // Row 7: 0, ., =
        grid.add(createKeyButton("0", Theme.HEX_SURFACE, Theme.HEX_WHITE, 18, true), 0, 7);
        grid.add(createKeyButton(".", Theme.HEX_SURFACE, Theme.HEX_WHITE, 20, true), 1, 7);
        grid.add(createKeyButton("=", Theme.HEX_TERRACOTTA, Theme.HEX_WHITE, 20, false), 2, 7);

        return grid;
    }

    private Button createKeyButton(String label, String bgHex, String textHex, int fontSize, boolean isMono) {
        Button btn = new Button(label);
        btn.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        Theme.styleCalcButton(btn, bgHex, textHex, fontSize, isMono);
        btn.setOnAction(e -> processInput(label));
        keyButtonMap.put(label, btn);
        return btn;
    }

    public void handleKeyEvent(KeyEvent event) {
        if (event.getEventType() != KeyEvent.KEY_PRESSED) {
            return;
        }
        KeyCode code = event.getCode();
        String text = event.getText();

        if (code == KeyCode.ESCAPE) {
            processInput("AC");
        } else if (code == KeyCode.BACK_SPACE) {
            processInput("CE");
        } else if (code == KeyCode.ENTER) {
            processInput("=");
        } else if ( "+".equals(text) || code == KeyCode.ADD ) {
            processInput("+");
        } else if ( "=".equals(text) ) {
            processInput("=");
        } else if ( "-".equals(text) || code == KeyCode.SUBTRACT ) {
            processInput("-");
        } else if ( "*".equals(text) || code == KeyCode.MULTIPLY ) {
            processInput("×");
        } else if ( "/".equals(text) || code == KeyCode.DIVIDE ) {
            processInput("÷");
        } else if ( "%".equals(text) ) {
            processInput("%");
        } else if ( "^".equals(text) ) {
            processInput("^");
        } else if ( "(".equals(text) ) {
            processInput("(");
        } else if ( ")".equals(text) ) {
            processInput(")");
        } else if ( ".".equals(text) || code == KeyCode.DECIMAL ) {
            processInput(".");
        } else if (text != null && text.length() == 1 && Character.isDigit(text.charAt(0))) {
            processInput(text);
        }
    }

    private void processInput(String token) {
        Theme.triggerKeypressVisual(keyButtonMap.get(token));

        if ("AC".equals(token) || "CE".equals(token)) {
            konamiBuffer.setLength(0);
            if ("AC".equals(token)) {
                resetCalculator();
            } else {
                handleClearEntry();
            }
            return;
        }

        konamiBuffer.append(token);
        if (konamiBuffer.length() > 32) {
            konamiBuffer.delete(0, konamiBuffer.length() - 32);
        }

        if (konamiBuffer.toString().endsWith(KONAMI_CODE)) {
            konamiBuffer.setLength(0);
            resetCalculator();
            if (onUnlockMania != null) {
                onUnlockMania.run();
            }
            return;
        }

        if (showingError) {
            resetCalculator();
        }

        if ("=".equals(token)) {
            evaluateCurrentExpression();
            return;
        }

        if (isFunctionToken(token)) {
            if (showingResult) {
                String prevResult = resultLabel.getText();
                currentExpression.setLength(0);
                currentExpression.append(token).append("(").append(prevResult).append(")");
                showingResult = false;
            } else {
                currentExpression.append(token).append("(");
            }
            updateDisplayDuringInput();
            return;
        }

        if (showingResult) {
            if (isOperatorToken(token)) {
                String prevResult = resultLabel.getText();
                currentExpression.setLength(0);
                currentExpression.append(prevResult).append(token);
            } else {
                currentExpression.setLength(0);
                currentExpression.append(token);
            }
            showingResult = false;
        } else {
            currentExpression.append(token);
        }

        updateDisplayDuringInput();
    }

    private void handleClearEntry() {
        if (showingError || showingResult) {
            resetCalculator();
            return;
        }
        if (currentExpression.length() > 0) {
            currentExpression.deleteCharAt(currentExpression.length() - 1);
            updateDisplayDuringInput();
        }
    }

    private void evaluateCurrentExpression() {
        if (currentExpression.length() == 0) {
            return;
        }
        String expr = currentExpression.toString();
        if ("1+1".equals(expr)) {
            expressionLabel.setText("1+1 =");
            resultLabel.setText("i miss you");
            currentExpression.setLength(0);
            showingResult = true;
            return;
        }
        try {
            double value = evaluator.evaluate(expr);
            String formatted = formatNumber(value);
            expressionLabel.setText(expr + " =");
            resultLabel.setText(formatted);
            currentExpression.setLength(0);
            currentExpression.append(formatted);
            showingResult = true;
        } catch (Exception ex) {
            expressionLabel.setText(expr);
            resultLabel.setText("Error");
            currentExpression.setLength(0);
            showingError = true;
        }
    }

    private void updateDisplayDuringInput() {
        if (currentExpression.length() == 0) {
            expressionLabel.setText("");
            resultLabel.setText("0");
        } else {
            expressionLabel.setText(currentExpression.toString());
            resultLabel.setText(currentExpression.toString());
        }
    }

    public void resetCalculator() {
        currentExpression.setLength(0);
        showingResult = false;
        showingError = false;
        expressionLabel.setText("");
        resultLabel.setText("0");
    }

    private boolean isFunctionToken(String token) {
        return switch (token) {
            case "sin", "cos", "tan", "log", "ln", "sqrt", "abs" -> true;
            default -> false;
        };
    }

    private boolean isOperatorToken(String token) {
        return switch (token) {
            case "+", "-", "×", "÷", "%", "^" -> true;
            default -> false;
        };
    }

    private String formatNumber(double value) {
        if (Math.abs(value) < 1e-12) {
            value = 0.0;
        }
        BigDecimal bd = BigDecimal.valueOf(value).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros();
        return bd.toPlainString();
    }
}
