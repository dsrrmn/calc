package com.calculator.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Calculator extends Application {

    private CalculatorView calculatorView;
    private ManiaGameView maniaGameView;

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + Theme.HEX_BG + ";");

        calculatorView = new CalculatorView(this::switchToMania);
        maniaGameView = new ManiaGameView(this::switchToCalculator);

        maniaGameView.setVisible(false);
        maniaGameView.setManaged(false);

        root.getChildren().addAll(calculatorView, maniaGameView);

        Scene scene = new Scene(root, 480, 660);
        scene.setFill(Theme.COLOR_BG);

        scene.addEventFilter(KeyEvent.ANY, event -> {
            if (maniaGameView.isVisible()) {
                maniaGameView.handleKeyEvent(event);
            } else if (calculatorView.isVisible()) {
                calculatorView.handleKeyEvent(event);
            }
        });

        primaryStage.setTitle("calculator ni bogart");
        primaryStage.setMinWidth(420);
        primaryStage.setMinHeight(580);
        primaryStage.setScene(scene);
        primaryStage.show();
        root.requestFocus();
    }

    private void switchToMania() {
        calculatorView.setVisible(false);
        calculatorView.setManaged(false);

        maniaGameView.setVisible(true);
        maniaGameView.setManaged(true);
        maniaGameView.showDifficultyMenu();
        maniaGameView.requestFocus();
    }

    private void switchToCalculator() {
        maniaGameView.stopGameLoop();
        maniaGameView.setVisible(false);
        maniaGameView.setManaged(false);

        calculatorView.resetCalculator();
        calculatorView.setVisible(true);
        calculatorView.setManaged(true);
        calculatorView.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
