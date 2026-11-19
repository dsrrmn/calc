package com.calculator.app;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.Collections;
import java.util.List;

public class ManiaGameView extends BorderPane {

    private enum ScreenState {
        MENU, PLAYING, RESULTS
    }

    private static final String[] LANE_KEYS = {"D", "F", "J", "K"};
    private static final long RUN_TOTAL_DURATION_MS = 60000L;
    private static final long PERFECT_WINDOW_MS = 35L;
    private static final long GREAT_WINDOW_MS = 75L;
    private static final long GOOD_WINDOW_MS = 120L;

    private final GameSettings settings;
    private final ChartGenerator chartGenerator;
    private final Runnable onExitToCalculator;

    private final BorderPane topBar;
    private final Label navTitleLabel;
    private final Canvas canvas;

    private final VBox difficultyMenuOverlay;
    private final VBox resultsOverlay;
    private final StackPane pauseOverlay;
    private final StackPane optionsOverlay;

    private Label speedValueLabel;
    private Button tilesToggleBtn;
    private Button circlesToggleBtn;

    private Label gradeValueLabel;
    private Label finalScoreLabel;
    private Label finalAccuracyLabel;
    private Label finalComboLabel;
    private Label judgmentBreakdownLabel;

    private ScreenState screenState = ScreenState.MENU;
    private Difficulty currentDifficulty = Difficulty.MEDIUM;
    private List<Note> notes = Collections.emptyList();
    private final boolean[] lanePressed = new boolean[4];

    private AnimationTimer gameTimer;
    private long startNanoTime;
    private long pauseStartNanoTime;
    private long accumulatedPauseNanos;
    private long currentElapsedMs;
    private boolean isPaused;

    private int score;
    private int combo;
    private int maxCombo;
    private int perfectCount;
    private int greatCount;
    private int goodCount;
    private int missCount;

    private String lastJudgmentText = "";
    private Color lastJudgmentColor = Theme.COLOR_WHITE;
    private long lastJudgmentTimeMs = -1000L;

    private String speedToastText = "";
    private long speedToastExpireNano = 0L;

    public ManiaGameView(Runnable onExitToCalculator) {
        this.onExitToCalculator = onExitToCalculator;
        this.settings = new GameSettings();
        this.chartGenerator = new ChartGenerator();

        setStyle("-fx-background-color: " + Theme.HEX_BG + ";");

        // Top Navigation Bar (Visible ONLY on Difficulty Selection Menu)
        topBar = new BorderPane();
        topBar.setPadding(new Insets(12, 24, 12, 24));
        topBar.setStyle("-fx-background-color: " + Theme.HEX_BG + ";");

        navTitleLabel = new Label("4K Mania");
        navTitleLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: bold;");
        BorderPane.setAlignment(navTitleLabel, Pos.CENTER_LEFT);

        Button optionsButton = new Button("Options");
        Theme.styleNavButton(optionsButton, Theme.HEX_SURFACE);
        optionsButton.setOnAction(e -> openOptionsModal());

        Button exitButton = new Button("Exit");
        Theme.styleNavButton(exitButton, Theme.HEX_RUST);
        exitButton.setOnAction(e -> exitToCalculator());

        HBox navButtons = new HBox(Theme.SPACE_8, optionsButton, exitButton);
        navButtons.setAlignment(Pos.CENTER_RIGHT);

        topBar.setLeft(navTitleLabel);
        topBar.setRight(navButtons);
        setTop(topBar);

        // Full-Height Canvas & Layered Overlays Stack
        StackPane centerStack = new StackPane();
        centerStack.setMinSize(0, 0);
        centerStack.setStyle("-fx-background-color: " + Theme.HEX_BG + ";");

        Pane canvasContainer = new Pane();
        canvasContainer.setMinSize(0, 0);
        canvas = new Canvas();
        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());
        canvas.widthProperty().addListener((obs, oldV, newV) -> renderCanvas());
        canvas.heightProperty().addListener((obs, oldV, newV) -> renderCanvas());
        canvasContainer.getChildren().add(canvas);

        difficultyMenuOverlay = buildDifficultyMenuOverlay();
        resultsOverlay = buildResultsOverlay();
        pauseOverlay = buildPauseOverlay();
        optionsOverlay = buildOptionsOverlay();

        // Layer order: Canvas -> Menu -> Results -> Pause Overlay -> Options Overlay (topmost)
        centerStack.getChildren().addAll(
                canvasContainer,
                difficultyMenuOverlay,
                resultsOverlay,
                pauseOverlay,
                optionsOverlay
        );
        setCenter(centerStack);

        initGameTimer();
        showDifficultyMenu();
    }

    private VBox buildDifficultyMenuOverlay() {
        VBox box = new VBox(10);
        box.setMaxWidth(380);
        box.setMaxHeight(Region.USE_PREF_SIZE);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: transparent;");

        Label header = new Label("Select Difficulty");
        header.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 20px; -fx-font-weight: bold;");
        VBox.setMargin(header, new Insets(0, 0, 10, 0));
        box.getChildren().add(header);

        for (Difficulty diff : Difficulty.values()) {
            Button btn = new Button(diff.getDisplayName());
            btn.setPrefHeight(46);
            btn.setMaxWidth(Double.MAX_VALUE);
            Theme.styleDifficultyButton(btn);
            btn.setOnAction(e -> startRun(diff));
            box.getChildren().add(btn);
        }

        Label footerHint = new Label("D  F  J  K   •   Ctrl + O for Options");
        footerHint.setStyle("-fx-text-fill: #8F8E8A; -fx-font-size: 12px;");
        VBox.setMargin(footerHint, new Insets(10, 0, 0, 0));
        box.getChildren().add(footerHint);

        return box;
    }

    private StackPane buildPauseOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: #000000CC;");
        overlay.setAlignment(Pos.CENTER);
        overlay.setVisible(false);
        overlay.setManaged(false);

        VBox card = new VBox(12);
        card.setMaxWidth(320);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(24));
        card.setAlignment(Pos.CENTER);
        card.setStyle(
                "-fx-background-color: #2C2B29; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-color: #3D3B38 #242321 #181716 #242321; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 10px;"
        );

        Label title = new Label("Paused");
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 20px; -fx-font-weight: bold;");
        VBox.setMargin(title, new Insets(0, 0, 4, 0));

        Button resumeBtn = createPauseButton("Resume", "#B2633A", "#C26F44", "#FFFFFF");
        resumeBtn.setOnAction(e -> resumeGame());

        Button retryBtn = createPauseButton("Retry", "#000000", "#B2633A", "#FFFFFF");
        retryBtn.setOnAction(e -> {
            pauseOverlay.setVisible(false);
            pauseOverlay.setManaged(false);
            isPaused = false;
            startRun(currentDifficulty);
        });

        Button optionsBtn = createPauseButton("Options", "#000000", "#B2633A", "#FFFFFF");
        optionsBtn.setOnAction(e -> openOptionsModal());

        Button diffMenuBtn = createPauseButton("Difficulty Menu", "#000000", "#B2633A", "#FFFFFF");
        diffMenuBtn.setOnAction(e -> {
            pauseOverlay.setVisible(false);
            pauseOverlay.setManaged(false);
            isPaused = false;
            showDifficultyMenu();
        });

        Button exitBtn = createPauseButton("Exit to Calculator", "#7E3C21", "#914627", "#FFFFFF");
        exitBtn.setOnAction(e -> exitToCalculator());

        card.getChildren().addAll(title, resumeBtn, retryBtn, optionsBtn, diffMenuBtn, exitBtn);
        overlay.getChildren().add(card);
        return overlay;
    }

    private StackPane buildOptionsOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: #000000CC;");
        overlay.setAlignment(Pos.CENTER);
        overlay.setVisible(false);
        overlay.setManaged(false);

        VBox card = new VBox(18);
        card.setMaxWidth(360);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(24));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: #2C2B29; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-color: #3D3B38 #242321 #181716 #242321; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 10px;"
        );

        Label title = new Label("Options");
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Scroll Speed Row
        HBox speedRow = new HBox(12);
        speedRow.setAlignment(Pos.CENTER_LEFT);
        Label speedLabel = new Label("Scroll Speed");
        speedLabel.setStyle("-fx-text-fill: #8F8E8A; -fx-font-size: 14px; -fx-font-weight: bold;");
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Button minusBtn = new Button("-");
        minusBtn.setFocusTraversable(false);
        minusBtn.setCursor(Cursor.HAND);
        styleStepperButton(minusBtn);
        minusBtn.setOnAction(e -> {
            settings.adjustScrollSpeed(-0.1);
            refreshOptionsUI();
            renderCanvas();
        });

        speedValueLabel = new Label("1.6x");
        speedValueLabel.setMinWidth(48);
        speedValueLabel.setAlignment(Pos.CENTER);
        speedValueLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: monospace;");

        Button plusBtn = new Button("+");
        plusBtn.setFocusTraversable(false);
        plusBtn.setCursor(Cursor.HAND);
        styleStepperButton(plusBtn);
        plusBtn.setOnAction(e -> {
            settings.adjustScrollSpeed(0.1);
            refreshOptionsUI();
            renderCanvas();
        });

        speedRow.getChildren().addAll(speedLabel, spacer1, minusBtn, speedValueLabel, plusBtn);

        // Note Style Row
        HBox styleRow = new HBox(12);
        styleRow.setAlignment(Pos.CENTER_LEFT);
        Label styleLabel = new Label("Note Style");
        styleLabel.setStyle("-fx-text-fill: #8F8E8A; -fx-font-size: 14px; -fx-font-weight: bold;");
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        tilesToggleBtn = new Button("Tiles");
        tilesToggleBtn.setFocusTraversable(false);
        tilesToggleBtn.setCursor(Cursor.HAND);
        tilesToggleBtn.setMinSize(88, 34);
        tilesToggleBtn.setOnAction(e -> {
            settings.setUseCircles(false);
            refreshOptionsUI();
            renderCanvas();
        });

        circlesToggleBtn = new Button("Circles");
        circlesToggleBtn.setFocusTraversable(false);
        circlesToggleBtn.setCursor(Cursor.HAND);
        circlesToggleBtn.setMinSize(88, 34);
        circlesToggleBtn.setOnAction(e -> {
            settings.setUseCircles(true);
            refreshOptionsUI();
            renderCanvas();
        });

        HBox toggleGroup = new HBox(6, tilesToggleBtn, circlesToggleBtn);
        toggleGroup.setAlignment(Pos.CENTER_RIGHT);

        styleRow.getChildren().addAll(styleLabel, spacer2, toggleGroup);

        Button doneBtn = new Button("Done");
        doneBtn.setFocusTraversable(false);
        doneBtn.setCursor(Cursor.HAND);
        doneBtn.setMaxWidth(Double.MAX_VALUE);
        doneBtn.setMinHeight(40);
        String doneBase = "-fx-background-color: #B2633A; -fx-text-fill: #FFFFFF; -fx-background-radius: 6px; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-insets: 0; -fx-border-width: 0;";
        String doneHover = "-fx-background-color: #C26F44; -fx-text-fill: #FFFFFF; -fx-background-radius: 6px; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-insets: 0; -fx-border-width: 0;";
        doneBtn.setStyle(doneBase);
        doneBtn.setOnMouseEntered(e -> doneBtn.setStyle(doneHover));
        doneBtn.setOnMouseExited(e -> doneBtn.setStyle(doneBase));
        doneBtn.setOnAction(e -> closeOptionsModal());

        card.getChildren().addAll(title, speedRow, styleRow, doneBtn);
        refreshOptionsUI();
        overlay.getChildren().add(card);
        return overlay;
    }

    private void styleStepperButton(Button btn) {
        String base = "-fx-background-color: #000000; -fx-text-fill: #FFFFFF; -fx-background-radius: 6px; -fx-min-width: 34px; -fx-min-height: 30px; -fx-font-weight: bold; -fx-font-size: 15px; -fx-background-insets: 0; -fx-border-width: 0;";
        String hover = "-fx-background-color: #B2633A; -fx-text-fill: #FFFFFF; -fx-background-radius: 6px; -fx-min-width: 34px; -fx-min-height: 30px; -fx-font-weight: bold; -fx-font-size: 15px; -fx-background-insets: 0; -fx-border-width: 0;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    private Button createPauseButton(String text, String bgHex, String hoverBgHex, String textHex) {
        Button btn = new Button(text);
        btn.setFocusTraversable(false);
        btn.setCursor(Cursor.HAND);
        btn.setPrefHeight(42);
        btn.setMaxWidth(Double.MAX_VALUE);

        String baseStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 6px; " +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-insets: 0; -fx-border-width: 0;",
                bgHex, textHex
        );
        String hoverStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 6px; " +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-insets: 0; -fx-border-width: 0;",
                hoverBgHex, textHex
        );

        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        return btn;
    }

    private VBox buildResultsOverlay() {
        VBox card = new VBox(Theme.SPACE_16);
        card.setMaxWidth(380);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setAlignment(Pos.CENTER);
        Theme.styleCard(card, Theme.RADIUS_BASE, Theme.SPACE_24);

        Label header = new Label("RUN COMPLETE");
        header.setStyle("-fx-text-fill: #8F8E8A; -fx-font-size: 12px; -fx-font-weight: bold;");

        gradeValueLabel = new Label("S");
        gradeValueLabel.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-size: 52px; -fx-font-family: %s; -fx-font-weight: 700;",
                Theme.HEX_TERRACOTTA, Theme.FONT_SANS
        ));

        finalScoreLabel = new Label("Score: 0");
        finalScoreLabel.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-size: 20px; -fx-font-family: %s; -fx-font-weight: 600;",
                Theme.HEX_WHITE, Theme.FONT_MONO
        ));

        finalAccuracyLabel = new Label("Accuracy: 100.0%");
        finalAccuracyLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: bold;");

        finalComboLabel = new Label("Max Combo: 0");
        finalComboLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: bold;");

        VBox statsBox = new VBox(Theme.SPACE_4, finalAccuracyLabel, finalComboLabel);
        statsBox.setAlignment(Pos.CENTER);

        judgmentBreakdownLabel = new Label("");
        judgmentBreakdownLabel.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-size: 12px; -fx-font-family: %s;",
                Theme.HEX_STONE, Theme.FONT_MONO
        ));

        HBox actionRow = new HBox(Theme.SPACE_8);
        actionRow.setAlignment(Pos.CENTER);

        Button retryBtn = new Button("Retry");
        retryBtn.setMinHeight(40);
        retryBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(retryBtn, Priority.ALWAYS);
        Theme.styleFlatButton(retryBtn, Theme.HEX_TERRACOTTA, Theme.HEX_WHITE, 14, 6);
        retryBtn.setOnAction(e -> startRun(currentDifficulty));

        Button diffMenuBtn = new Button("Difficulty Menu");
        diffMenuBtn.setMinHeight(40);
        diffMenuBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(diffMenuBtn, Priority.ALWAYS);
        Theme.styleFlatButton(diffMenuBtn, Theme.HEX_SURFACE, Theme.HEX_WHITE, 14, 6);
        diffMenuBtn.setOnAction(e -> showDifficultyMenu());

        actionRow.getChildren().addAll(retryBtn, diffMenuBtn);

        card.getChildren().addAll(
                header,
                gradeValueLabel,
                finalScoreLabel,
                statsBox,
                judgmentBreakdownLabel,
                actionRow
        );
        card.setVisible(false);
        card.setManaged(false);
        return card;
    }

    private void refreshOptionsUI() {
        speedValueLabel.setText(String.format("%.1fx", settings.getScrollSpeed()));
        String active = "-fx-background-color: #B2633A; -fx-text-fill: #FFFFFF; -fx-background-radius: 6px; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-insets: 0; -fx-border-width: 0;";
        String inactive = "-fx-background-color: #000000; -fx-text-fill: #8F8E8A; -fx-background-radius: 6px; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-insets: 0; -fx-border-width: 0;";

        if (settings.isUseCircles()) {
            tilesToggleBtn.setStyle(inactive);
            circlesToggleBtn.setStyle(active);
        } else {
            tilesToggleBtn.setStyle(active);
            circlesToggleBtn.setStyle(inactive);
        }
    }

    private void initGameTimer() {
        gameTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isPaused || screenState != ScreenState.PLAYING) {
                    return;
                }
                currentElapsedMs = (now - startNanoTime - accumulatedPauseNanos) / 1_000_000L;
                updateNotes(currentElapsedMs);
                renderCanvas();

                if (currentElapsedMs >= RUN_TOTAL_DURATION_MS || allNotesResolved()) {
                    finishRun();
                }
            }
        };
    }

    public void showDifficultyMenu() {
        stopGameLoop();
        screenState = ScreenState.MENU;
        isPaused = false;

        // Restore topBar on Difficulty Selection Menu
        topBar.setVisible(true);
        topBar.setManaged(true);
        navTitleLabel.setText("4K Mania");

        difficultyMenuOverlay.setVisible(true);
        difficultyMenuOverlay.setManaged(true);
        resultsOverlay.setVisible(false);
        resultsOverlay.setManaged(false);
        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);
        optionsOverlay.setVisible(false);
        optionsOverlay.setManaged(false);

        clearLanePresses();
        renderCanvas();
    }

    private void startRun(Difficulty difficulty) {
        stopGameLoop();
        this.currentDifficulty = difficulty;
        this.notes = chartGenerator.generateChart(difficulty);
        this.screenState = ScreenState.PLAYING;
        this.isPaused = false;

        this.score = 0;
        this.combo = 0;
        this.maxCombo = 0;
        this.perfectCount = 0;
        this.greatCount = 0;
        this.goodCount = 0;
        this.missCount = 0;
        this.lastJudgmentText = "";
        this.lastJudgmentTimeMs = -1000L;
        this.currentElapsedMs = 0L;

        // Hide topBar during active gameplay so Canvas occupies 100% window height
        topBar.setVisible(false);
        topBar.setManaged(false);

        difficultyMenuOverlay.setVisible(false);
        difficultyMenuOverlay.setManaged(false);
        resultsOverlay.setVisible(false);
        resultsOverlay.setManaged(false);
        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);
        optionsOverlay.setVisible(false);
        optionsOverlay.setManaged(false);

        clearLanePresses();
        startNanoTime = System.nanoTime();
        accumulatedPauseNanos = 0L;
        gameTimer.start();
    }

    private void pauseGame() {
        if (screenState != ScreenState.PLAYING || isPaused) {
            return;
        }
        isPaused = true;
        pauseStartNanoTime = System.nanoTime();
        stopGameLoop();
        clearLanePresses();
        pauseOverlay.setVisible(true);
        pauseOverlay.setManaged(true);
        renderCanvas();
    }

    private void resumeGame() {
        if (!isPaused) {
            return;
        }
        accumulatedPauseNanos += (System.nanoTime() - pauseStartNanoTime);
        isPaused = false;
        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);
        optionsOverlay.setVisible(false);
        optionsOverlay.setManaged(false);
        clearLanePresses();
        gameTimer.start();
    }

    private void finishRun() {
        stopGameLoop();
        screenState = ScreenState.RESULTS;
        isPaused = false;
        clearLanePresses();

        double accuracy = calculateAccuracy();
        String grade = computeLetterGrade(accuracy);

        gradeValueLabel.setText(grade);
        finalScoreLabel.setText(String.format("Score: %,d", score));
        finalAccuracyLabel.setText(String.format("Accuracy: %.1f%%", accuracy));
        finalComboLabel.setText("Max Combo: " + maxCombo);
        judgmentBreakdownLabel.setText(String.format(
                "PERFECT: %d   GREAT: %d   GOOD: %d   MISS: %d",
                perfectCount, greatCount, goodCount, missCount
        ));

        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);
        optionsOverlay.setVisible(false);
        optionsOverlay.setManaged(false);
        resultsOverlay.setVisible(true);
        resultsOverlay.setManaged(true);
        renderCanvas();
    }

    private void openOptionsModal() {
        if (screenState == ScreenState.PLAYING && !isPaused) {
            pauseGame();
        }
        refreshOptionsUI();
        optionsOverlay.setVisible(true);
        optionsOverlay.setManaged(true);
        renderCanvas();
    }

    private void closeOptionsModal() {
        optionsOverlay.setVisible(false);
        optionsOverlay.setManaged(false);
        // Does not unpause gameplay behind it
        renderCanvas();
    }

    private void toggleOptionsModal() {
        if (optionsOverlay.isVisible()) {
            closeOptionsModal();
        } else {
            openOptionsModal();
        }
    }

    private void exitToCalculator() {
        stopGameLoop();
        isPaused = false;
        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);
        optionsOverlay.setVisible(false);
        optionsOverlay.setManaged(false);
        clearLanePresses();
        if (onExitToCalculator != null) {
            onExitToCalculator.run();
        }
    }

    public void stopGameLoop() {
        if (gameTimer != null) {
            gameTimer.stop();
        }
    }

    private void clearLanePresses() {
        for (int i = 0; i < 4; i++) {
            lanePressed[i] = false;
        }
    }

    public void handleKeyEvent(KeyEvent event) {
        KeyCode code = event.getCode();

        if (event.getEventType() == KeyEvent.KEY_PRESSED) {
            // 1. ESC handling: Top-down modal dismissal
            if (code == KeyCode.ESCAPE) {
                if (optionsOverlay.isVisible()) {
                    closeOptionsModal();
                    event.consume();
                    return;
                }
                if (pauseOverlay.isVisible()) {
                    resumeGame();
                    event.consume();
                    return;
                }
                if (screenState == ScreenState.PLAYING) {
                    pauseGame();
                    event.consume();
                    return;
                }
                if (screenState == ScreenState.RESULTS) {
                    showDifficultyMenu();
                    event.consume();
                    return;
                }
                if (screenState == ScreenState.MENU) {
                    exitToCalculator();
                    event.consume();
                    return;
                }
            }

            // 2. Ctrl + O for Options
            if (code == KeyCode.O && event.isControlDown()) {
                toggleOptionsModal();
                event.consume();
                return;
            }

            // 3. F3/F4 and -/= for live scroll speed adjustments
            if (code == KeyCode.F3 || (!event.isControlDown() && code == KeyCode.MINUS)) {
                settings.adjustScrollSpeed(-0.1);
                refreshOptionsUI();
                triggerSpeedToast();
                renderCanvas();
                return;
            }

            if (code == KeyCode.F4 || (!event.isControlDown() && code == KeyCode.EQUALS)) {
                settings.adjustScrollSpeed(0.1);
                refreshOptionsUI();
                triggerSpeedToast();
                renderCanvas();
                return;
            }

            // 4. Lane key inputs (only during active gameplay when not paused)
            if (screenState == ScreenState.PLAYING && !isPaused && !optionsOverlay.isVisible()) {
                int lane = mapKeyToLane(code);
                if (lane != -1 && !lanePressed[lane]) {
                    lanePressed[lane] = true;
                    processLaneHit(lane);
                    renderCanvas();
                }
            }
        } else if (event.getEventType() == KeyEvent.KEY_RELEASED) {
            int lane = mapKeyToLane(code);
            if (lane != -1) {
                lanePressed[lane] = false;
                if (screenState != ScreenState.PLAYING || isPaused) {
                    renderCanvas();
                }
            }
        }
    }

    private void triggerSpeedToast() {
        speedToastText = String.format("Scroll Speed: %.1fx", settings.getScrollSpeed());
        speedToastExpireNano = System.nanoTime() + 1_200_000_000L;
    }

    private int mapKeyToLane(KeyCode code) {
        return switch (code) {
            case D -> 0;
            case F -> 1;
            case J -> 2;
            case K -> 3;
            default -> -1;
        };
    }

    private void processLaneHit(int lane) {
        Note target = null;
        long smallestDiff = Long.MAX_VALUE;

        for (Note note : notes) {
            if (note.getLane() == lane && note.isActive()) {
                long diff = Math.abs(note.getTimeMs() - currentElapsedMs);
                if (diff <= GOOD_WINDOW_MS && diff < smallestDiff) {
                    smallestDiff = diff;
                    target = note;
                }
            }
        }

        if (target == null) {
            return;
        }

        target.setHit(true);
        combo++;
        if (combo > maxCombo) {
            maxCombo = combo;
        }

        if (smallestDiff <= PERFECT_WINDOW_MS) {
            score += 300;
            perfectCount++;
            setJudgmentFeedback("PERFECT", Theme.COLOR_TERRACOTTA);
        } else if (smallestDiff <= GREAT_WINDOW_MS) {
            score += 200;
            greatCount++;
            setJudgmentFeedback("GREAT", Theme.COLOR_WHITE);
        } else {
            score += 100;
            goodCount++;
            setJudgmentFeedback("GOOD", Theme.COLOR_STONE);
        }
    }

    private void updateNotes(long elapsedMs) {
        double speedPxPerMs = settings.getScrollSpeedPxPerMs();
        for (Note note : notes) {
            if (!note.isActive()) {
                continue;
            }
            double deltaPx = (elapsedMs - note.getTimeMs()) * speedPxPerMs;
            if (deltaPx > 45.0) {
                note.setMissed(true);
                combo = 0;
                missCount++;
                setJudgmentFeedback("MISS", Theme.COLOR_RUST);
            }
        }
    }

    private boolean allNotesResolved() {
        for (Note note : notes) {
            if (note.isActive()) {
                return false;
            }
        }
        return !notes.isEmpty();
    }

    private void setJudgmentFeedback(String text, Color color) {
        this.lastJudgmentText = text;
        this.lastJudgmentColor = color;
        this.lastJudgmentTimeMs = currentElapsedMs;
    }

    private double calculateAccuracy() {
        int totalJudged = perfectCount + greatCount + goodCount + missCount;
        if (totalJudged == 0) {
            return 100.0;
        }
        double weighted = (perfectCount * 300.0) + (greatCount * 200.0) + (goodCount * 100.0);
        return (weighted / (totalJudged * 300.0)) * 100.0;
    }

    private String computeLetterGrade(double accuracy) {
        if (accuracy >= 95.0) return "S";
        if (accuracy >= 88.0) return "A";
        if (accuracy >= 78.0) return "B";
        if (accuracy >= 68.0) return "C";
        return "D";
    }

    private void renderCanvas() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Theme.COLOR_BG);
        gc.fillRect(0, 0, w, h);

        if (screenState == ScreenState.MENU) {
            return;
        }

        double trackWidth = Math.min(w * 0.65, 360.0);
        double trackX = (w - trackWidth) / 2.0;
        double laneWidth = trackWidth / 4.0;
        double hitLineY = h - 65.0;

        // Track background & subtle lane separators
        gc.setFill(Color.web("#13151A"));
        gc.fillRect(trackX, 0, trackWidth, h);

        gc.setStroke(Theme.COLOR_BORDER);
        gc.setLineWidth(1.0);
        for (int i = 0; i <= 4; i++) {
            double lx = trackX + i * laneWidth;
            gc.strokeLine(lx, 0, lx, h);
        }

        // Judgment Line
        gc.setStroke(Theme.COLOR_TERRACOTTA);
        gc.setLineWidth(2.0);
        gc.strokeLine(trackX, hitLineY, trackX + trackWidth, hitLineY);

        // Receptors at hitLineY
        boolean circles = settings.isUseCircles();
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        for (int lane = 0; lane < 4; lane++) {
            double laneLeft = trackX + lane * laneWidth;
            Color laneAccent = (lane == 0 || lane == 3) ? Theme.COLOR_STONE : Theme.COLOR_TERRACOTTA;
            Color laneSkirt = (lane == 0 || lane == 3) ? Color.web(Theme.STONE_SKIRT) : Color.web(Theme.TERRACOTTA_SKIRT);
            boolean pressed = lanePressed[lane];

            if (circles) {
                double diameter = laneWidth - 16.0;
                double cx = laneLeft + (laneWidth - diameter) / 2.0;
                double cy = hitLineY - diameter / 2.0;

                if (pressed) {
                    gc.setFill(laneSkirt);
                    gc.fillOval(cx, cy, diameter, diameter);
                    gc.setFill(laneAccent);
                    gc.fillOval(cx, cy + 1.0, diameter, diameter - 2.0);

                    gc.setStroke(Theme.COLOR_WHITE);
                    gc.setLineWidth(1.5);
                    gc.strokeOval(cx, cy, diameter, diameter);

                    gc.setFill(Theme.COLOR_WHITE);
                    gc.fillText(LANE_KEYS[lane], laneLeft + laneWidth / 2.0, hitLineY + 1.0);
                } else {
                    gc.setFill(Color.web(Theme.CHARCOAL_SKIRT));
                    gc.fillOval(cx, cy, diameter, diameter);

                    gc.setFill(Color.web(Theme.CHARCOAL_HIGHLIGHT));
                    gc.fillOval(cx, cy, diameter, diameter - 2.0);

                    gc.setFill(Theme.COLOR_SURFACE);
                    gc.fillOval(cx, cy + 1.0, diameter, diameter - 3.0);

                    gc.setStroke(laneAccent);
                    gc.setLineWidth(1.5);
                    gc.strokeOval(cx, cy, diameter, diameter);

                    gc.setFill(Theme.COLOR_STONE);
                    gc.fillText(LANE_KEYS[lane], laneLeft + laneWidth / 2.0, hitLineY);
                }
            } else {
                double tileW = laneWidth - 12.0;
                double tileH = 16.0;
                double rx = laneLeft + 6.0;
                double ry = hitLineY - tileH / 2.0;

                if (pressed) {
                    gc.setFill(laneSkirt);
                    gc.fillRoundRect(rx, ry, tileW, tileH, 4, 4);
                    gc.setFill(laneAccent);
                    gc.fillRoundRect(rx, ry + 1.0, tileW, tileH - 2.0, 3, 3);

                    gc.setFill(Theme.COLOR_WHITE);
                    gc.fillText(LANE_KEYS[lane], laneLeft + laneWidth / 2.0, hitLineY + 29.0);
                } else {
                    gc.setFill(Color.web(Theme.CHARCOAL_SKIRT));
                    gc.fillRoundRect(rx, ry, tileW, tileH, 4, 4);

                    gc.setFill(Color.web(Theme.CHARCOAL_HIGHLIGHT));
                    gc.fillRoundRect(rx, ry, tileW, tileH - 2.0, 4, 4);

                    gc.setFill(Theme.COLOR_SURFACE);
                    gc.fillRoundRect(rx, ry + 1.0, tileW, tileH - 3.0, 3, 3);

                    gc.setFill(Theme.COLOR_STONE);
                    gc.fillText(LANE_KEYS[lane], laneLeft + laneWidth / 2.0, hitLineY + 28.0);
                }
            }
        }

        // Falling Notes (Tactile layered hardware keycaps)
        double speedPxPerMs = settings.getScrollSpeedPxPerMs();
        for (Note note : notes) {
            if (!note.isActive()) {
                continue;
            }
            double y = hitLineY - ((note.getTimeMs() - currentElapsedMs) * speedPxPerMs);
            if (y < -60.0 || y > h + 60.0) {
                continue;
            }

            int lane = note.getLane();
            double laneLeft = trackX + lane * laneWidth;
            Color noteColor = (lane == 0 || lane == 3) ? Theme.COLOR_STONE : Theme.COLOR_TERRACOTTA;
            Color noteSkirt = (lane == 0 || lane == 3) ? Color.web(Theme.STONE_SKIRT) : Color.web(Theme.TERRACOTTA_SKIRT);
            Color noteHighlight = (lane == 0 || lane == 3) ? Color.web(Theme.STONE_HIGHLIGHT) : Color.web(Theme.TERRACOTTA_HIGHLIGHT);

            if (circles) {
                double diameter = laneWidth - 16.0;
                double nx = laneLeft + (laneWidth - diameter) / 2.0;
                double ny = y - diameter / 2.0;

                // Layer 0: 2px Darker bottom rim
                gc.setFill(noteSkirt);
                gc.fillOval(nx, ny, diameter, diameter);

                // Layer 1: 1px Lighter top highlight
                gc.setFill(noteHighlight);
                gc.fillOval(nx, ny, diameter, diameter - 2.0);

                // Layer 2: Main matte face
                gc.setFill(noteColor);
                gc.fillOval(nx, ny + 1.0, diameter, diameter - 3.0);
            } else {
                double tileW = laneWidth - 12.0;
                double tileH = 16.0;
                double nx = laneLeft + 6.0;
                double ny = y - tileH / 2.0;

                // Layer 0: 2px Darker bottom rim
                gc.setFill(noteSkirt);
                gc.fillRoundRect(nx, ny, tileW, tileH, 4, 4);

                // Layer 1: 1px Lighter top highlight
                gc.setFill(noteHighlight);
                gc.fillRoundRect(nx, ny, tileW, tileH - 2.0, 4, 4);

                // Layer 2: Main matte face
                gc.setFill(noteColor);
                gc.fillRoundRect(nx, ny + 1.0, tileW, tileH - 3.0, 3, 3);
            }
        }

        // Top HUD Counters
        gc.setTextBaseline(VPos.TOP);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        gc.setFill(Theme.COLOR_WHITE);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(String.format("Score: %,d", score), 24, 16);

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(String.format("Accuracy: %.1f%%", calculateAccuracy()), w - 24, 16);

        gc.setTextAlign(TextAlignment.CENTER);
        if (combo > 0) {
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
            gc.setFill(Theme.COLOR_WHITE);
            gc.fillText(combo + " COMBO", w / 2.0, 20);
        }

        if (!lastJudgmentText.isEmpty() && (currentElapsedMs - lastJudgmentTimeMs) <= 600L) {
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
            gc.setFill(lastJudgmentColor);
            gc.fillText(lastJudgmentText, w / 2.0, 52);
        }

        // Live Speed Adjustment Popup Toast (1.2s)
        if (System.nanoTime() < speedToastExpireNano) {
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            gc.setFill(Theme.COLOR_TERRACOTTA);
            gc.fillText(speedToastText, w / 2.0, 84);
        }
    }
}
