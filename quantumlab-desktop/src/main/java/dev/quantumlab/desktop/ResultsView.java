package dev.quantumlab.desktop;

import dev.quantumlab.simulation.Formatting;
import dev.quantumlab.simulation.MeasurementSession;
import dev.quantumlab.simulation.OutcomeStatistics;
import dev.quantumlab.simulation.StatisticalSummary;
import java.util.ArrayList;
import java.util.List;

/**
 * Repeated-trial results, already formatted for display. Immutable.
 *
 * @param trials trials performed so far
 * @param remaining how many more trials the session accepts
 * @param rows one row per outcome
 * @param verdict a plain-language reading of the statistics
 */
public record ResultsView(int trials, int remaining, List<Row> rows, String verdict) {

    /** One outcome's observed-versus-theoretical comparison. */
    public record Row(
            String label,
            String count,
            String observed,
            String theoretical,
            String standardError,
            String sigma) {}

    public ResultsView {
        rows = List.copyOf(rows);
    }

    static ResultsView of(MeasurementSession session) {
        StatisticalSummary summary = session.summary();
        List<Row> rows = new ArrayList<>(summary.outcomes().size());
        for (OutcomeStatistics o : summary.outcomes()) {
            rows.add(
                    new Row(
                            o.label(),
                            Long.toString(o.count()),
                            Formatting.percent(o.observed()),
                            Formatting.percent(o.theoretical()),
                            "± " + Formatting.percent(o.standardError()),
                            Formatting.sigma(o.zScore())));
        }
        String verdict;
        if (summary.trials() == 0) {
            verdict = "No trials yet.";
        } else if (summary.withinFiveSigma()) {
            verdict = "All outcomes are within 5σ of the theoretical probabilities.";
        } else {
            verdict =
                    "An outcome is more than 5σ from theory. This should essentially never"
                            + " happen by chance, so it indicates a bug.";
        }
        return new ResultsView(summary.trials(), session.remainingCapacity(), rows, verdict);
    }
}
