package dev.quantumlab.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A compact, append-only record of trial outcomes: one byte per trial, plus running tallies.
 *
 * <p>One million trials cost one megabyte, so even the largest session keeps its full history and
 * the UI can chart or export it without sampling.
 *
 * <p>Not thread-safe; owned by a single session.
 */
final class OutcomeLog {

    private final int outcomeCount;
    private final int maxTrials;
    private final long[] tally;
    private byte[] data = new byte[1024];
    private int count;

    OutcomeLog(int outcomeCount, int maxTrials) {
        if (outcomeCount < 1 || outcomeCount > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("outcomeCount out of range: " + outcomeCount);
        }
        this.outcomeCount = outcomeCount;
        this.maxTrials = maxTrials;
        this.tally = new long[outcomeCount];
    }

    void add(int outcome) {
        if (outcome < 0 || outcome >= outcomeCount) {
            throw new IllegalArgumentException("Outcome " + outcome + " out of range");
        }
        if (count >= maxTrials) {
            throw new IllegalStateException("Trial limit of " + maxTrials + " reached");
        }
        if (count == data.length) {
            data = Arrays.copyOf(data, Math.min(maxTrials, data.length * 2));
        }
        data[count++] = (byte) outcome;
        tally[outcome]++;
    }

    int count() {
        return count;
    }

    int remaining() {
        return maxTrials - count;
    }

    int outcomeAt(int index) {
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException("Trial index " + index + " out of range 0.." + count);
        }
        return data[index];
    }

    long[] tallyCopy() {
        return tally.clone();
    }

    void clear() {
        Arrays.fill(tally, 0L);
        count = 0;
    }

    /**
     * The running frequency of {@code outcome} after {@code n} trials, for a set of {@code n}
     * values thinned to at most {@code maxPoints} entries.
     *
     * <p>If there are no more trials than {@code maxPoints}, every {@code n} is reported.
     * Otherwise the sample points are spaced geometrically, so the noisy early trials, which are
     * where convergence is most visible, are not lost.
     */
    List<SeriesPoint> runningFrequency(int outcome, int maxPoints) {
        if (maxPoints < 2) {
            throw new IllegalArgumentException("maxPoints must be >= 2");
        }
        if (count == 0) {
            return List.of();
        }
        int[] targets = sampleTargets(count, maxPoints);
        List<SeriesPoint> points = new ArrayList<>(targets.length);
        long hits = 0;
        int t = 0;
        for (int i = 1; i <= count && t < targets.length; i++) {
            if (data[i - 1] == outcome) {
                hits++;
            }
            if (i == targets[t]) {
                points.add(new SeriesPoint(i, (double) hits / i));
                t++;
            }
        }
        return points;
    }

    /** Strictly increasing trial numbers in {@code [1, n]}, ending at {@code n}. */
    static int[] sampleTargets(int n, int maxPoints) {
        if (n <= maxPoints) {
            int[] all = new int[n];
            for (int i = 0; i < n; i++) {
                all[i] = i + 1;
            }
            return all;
        }
        int[] buffer = new int[maxPoints];
        int size = 0;
        long last = 0;
        for (int k = 0; k < maxPoints; k++) {
            long v = Math.round(Math.pow(n, (double) k / (maxPoints - 1)));
            v = Math.max(1, Math.min(n, v));
            if (v > last) {
                buffer[size++] = (int) v;
                last = v;
            }
        }
        return Arrays.copyOf(buffer, size);
    }
}
