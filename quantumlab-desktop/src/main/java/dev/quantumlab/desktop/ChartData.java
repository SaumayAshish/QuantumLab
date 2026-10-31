package dev.quantumlab.desktop;

import dev.quantumlab.simulation.ClassicalDecaySession;
import dev.quantumlab.simulation.MeasurementSession;
import dev.quantumlab.simulation.OutcomeStatistics;
import dev.quantumlab.simulation.SeriesPoint;
import dev.quantumlab.simulation.StatisticalSummary;
import java.util.ArrayList;
import java.util.List;

/**
 * Numbers behind the charts, as plain immutable data (probabilities in {@code [0, 1]}).
 *
 * @param labels outcome labels
 * @param observed observed frequency of each outcome
 * @param theoretical Born probability of each outcome
 * @param convergenceLabel the outcome whose running frequency is plotted
 * @param convergenceTheory that outcome's theoretical probability
 * @param convergence its running frequency: x is the number of trials so far, y the frequency of
 *     the outcome among them (empty before the first trial)
 */
public record ChartData(
    List<String> labels,
    List<Double> observed,
    List<Double> theoretical,
    String convergenceLabel,
    double convergenceTheory,
    List<SeriesPoint> convergence) {

    public ChartData {
        labels = List.copyOf(labels);
        observed = List.copyOf(observed);
        theoretical = List.copyOf(theoretical);
        convergence = List.copyOf(convergence);
    }

    /** The convergence plot follows the first outcome; for a qubit the other is its complement. */
    static ChartData of(MeasurementSession session, int maxPoints) {
        requireEnoughPoints(maxPoints);
        return build(session.labels(), session.summary(), session.runningFrequency(0, maxPoints));
    }

    /**
     * The same two charts for the classical decay experiment. The convergence plot follows the
     * first outcome, "not decayed".
     */
    static ChartData ofClassical(ClassicalDecaySession session, int maxPoints) {
        requireEnoughPoints(maxPoints);
        return build(
            ClassicalDecaySession.LABELS,
            session.summary(),
            session.runningFrequency(0, maxPoints));
    }

    private static void requireEnoughPoints(int maxPoints) {
        if (maxPoints < 2) {
            throw new IllegalArgumentException("A chart needs at least 2 points, got " + maxPoints);
        }
    }

    private static ChartData build(
        List<String> labels, StatisticalSummary summary, List<SeriesPoint> running) {
        List<Double> observed = new ArrayList<>(labels.size());
        List<Double> theoretical = new ArrayList<>(labels.size());
        for (OutcomeStatistics o : summary.outcomes()) {
            observed.add(o.observed());
            theoretical.add(o.theoretical());
        }
        return new ChartData(
            labels, observed, theoretical, labels.get(0), theoretical.get(0), running);
    }
}
