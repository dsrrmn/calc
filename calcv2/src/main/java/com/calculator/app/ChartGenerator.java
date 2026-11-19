package com.calculator.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ChartGenerator {

    private static final long START_DELAY_MS = 1800L;
    private static final long CHART_DURATION_MS = 58500L;

    public List<Note> generateChart(Difficulty difficulty) {
        List<Note> notes = new ArrayList<>();
        Random random = new Random();

        int stepMs = difficulty.getStepMs();
        double chordChance = difficulty.getChordProbability();
        long[] lastLaneTime = {-1000L, -1000L, -1000L, -1000L};
        int previousPrimaryLane = -1;

        for (long t = START_DELAY_MS; t <= CHART_DURATION_MS; t += stepMs) {
            if (random.nextDouble() < 0.12) {
                continue;
            }

            int lane = pickAvailableLane(random, t, stepMs, lastLaneTime, previousPrimaryLane);
            notes.add(new Note(lane, t));
            lastLaneTime[lane] = t;
            previousPrimaryLane = lane;

            if (random.nextDouble() < chordChance) {
                int secondLane = (lane + 1 + random.nextInt(3)) % 4;
                if (t - lastLaneTime[secondLane] >= stepMs * 2L) {
                    notes.add(new Note(secondLane, t));
                    lastLaneTime[secondLane] = t;
                }
            }
        }

        notes.sort(Comparator.comparingLong(Note::getTimeMs));
        return notes;
    }

    private int pickAvailableLane(Random random, long currentTime, int stepMs, long[] lastLaneTime, int prevLane) {
        int candidate = random.nextInt(4);
        for (int attempt = 0; attempt < 6; attempt++) {
            boolean recentlyUsed = (currentTime - lastLaneTime[candidate]) < (stepMs * 1.5);
            if (!recentlyUsed && (candidate != prevLane || random.nextDouble() < 0.25)) {
                return candidate;
            }
            candidate = (candidate + 1) % 4;
        }
        return candidate;
    }
}
