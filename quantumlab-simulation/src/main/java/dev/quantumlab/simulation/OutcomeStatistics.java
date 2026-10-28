package dev.quantumlab.simulation;

/**
 * Observed versus theoretical statistics for one outcome of a repeated experiment.
 *
 * @param label the outcome's display label
 * @param count how many trials produced it
 * @param observed {@code count / trials} (0 if there are no trials yet)
 * @param theoretical the probability the theory assigns
 * @param standardError {@code √(p(1−p)/N)}: the expected scatter of {@code observed} about {@code
 *     theoretical} for {@code N} independent trials (0 if there are no trials yet)
 * @param zScore {@code (observed − theoretical) / standardError}; 0 when the standard error is 0
 */
public record OutcomeStatistics(
        String label,
        long count,
        double observed,
        double theoretical,
        double standardError,
        double zScore) {}
