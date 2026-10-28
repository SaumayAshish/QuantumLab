package dev.quantumlab.simulation;

import java.util.ArrayList;
import java.util.List;

/**
 * Repeated-trial statistics: how closely observed frequencies track the theoretical probabilities.
 *
 * <p>Each trial is an independent draw, so the count of any one outcome is binomial and its
 * frequency has standard error {@code √(p(1−p)/N)}. About 68% of runs land within 1σ of the
 * theoretical value and fewer than one in a million outside 5σ, so a deviation beyond 5σ signals a
 * bug, not bad luck.
 *
 * @param trials number of trials performed
 * @param outcomes per-outcome statistics, in outcome order
 * @param withinFiveSigma whether every outcome is within 5 standard errors of its theoretical value
 */
public record StatisticalSummary(
        int trials, List<OutcomeStatistics> outcomes, boolean withinFiveSigma) {

    public StatisticalSummary {
        outcomes = List.copyOf(outcomes);
    }

    /**
     * Builds a summary from raw counts.
     *
     * @param labels outcome labels
     * @param counts how many times each outcome occurred
     * @param theoretical the theoretical probability of each outcome
     */
    public static StatisticalSummary of(
            List<String> labels, long[] counts, double[] theoretical) {
        long n = 0;
        for (long c : counts) {
            n += c;
        }
        List<OutcomeStatistics> stats = new ArrayList<>(counts.length);
        boolean within = true;
        for (int k = 0; k < counts.length; k++) {
            double p = theoretical[k];
            double observed = n == 0 ? 0.0 : (double) counts[k] / n;
            double se = n == 0 ? 0.0 : Math.sqrt(p * (1.0 - p) / n);
            double z = se == 0.0 ? 0.0 : (observed - p) / se;
            if (Math.abs(z) > 5.0) {
                within = false;
            }
            stats.add(new OutcomeStatistics(labels.get(k), counts[k], observed, p, se, z));
        }
        return new StatisticalSummary(Math.toIntExact(n), stats, within);
    }
}
