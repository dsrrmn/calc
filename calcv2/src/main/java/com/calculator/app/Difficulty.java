package com.calculator.app;

public enum Difficulty {
    BEGINNER("Beginner", 380, 0.05),
    EASY("Easy", 290, 0.10),
    MEDIUM("Medium", 220, 0.16),
    HARD("Hard", 165, 0.24),
    INSANE("Insane", 130, 0.32),
    EXTRA("Extra", 105, 0.40);

    private final String displayName;
    private final int stepMs;
    private final double chordProbability;

    Difficulty(String displayName, int stepMs, double chordProbability) {
        this.displayName = displayName;
        this.stepMs = stepMs;
        this.chordProbability = chordProbability;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getStepMs() {
        return stepMs;
    }

    public double getChordProbability() {
        return chordProbability;
    }
}
