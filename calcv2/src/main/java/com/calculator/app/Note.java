package com.calculator.app;

public class Note {

    private final int lane;
    private final long timeMs;
    private boolean hit;
    private boolean missed;

    public Note(int lane, long timeMs) {
        this.lane = lane;
        this.timeMs = timeMs;
        this.hit = false;
        this.missed = false;
    }

    public int getLane() {
        return lane;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public boolean isHit() {
        return hit;
    }

    public void setHit(boolean hit) {
        this.hit = hit;
    }

    public boolean isMissed() {
        return missed;
    }

    public void setMissed(boolean missed) {
        this.missed = missed;
    }

    public boolean isActive() {
        return !hit && !missed;
    }
}
