package com.calculator.app;

public class GameSettings {

    public static final double MIN_SPEED = 0.8;
    public static final double MAX_SPEED = 4.0;
    public static final double BASE_PX_PER_MS = 0.36;

    private double scrollSpeed = 1.6;
    private boolean useCircles = false;

    public double getScrollSpeed() {
        return scrollSpeed;
    }

    public void setScrollSpeed(double speed) {
        double clamped = Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
        this.scrollSpeed = Math.round(clamped * 10.0) / 10.0;
    }

    public void adjustScrollSpeed(double delta) {
        setScrollSpeed(this.scrollSpeed + delta);
    }

    public double getScrollSpeedPxPerMs() {
        return scrollSpeed * BASE_PX_PER_MS;
    }

    public boolean isUseCircles() {
        return useCircles;
    }

    public void setUseCircles(boolean useCircles) {
        this.useCircles = useCircles;
    }
}
