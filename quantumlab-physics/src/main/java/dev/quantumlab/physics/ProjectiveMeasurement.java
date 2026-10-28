package dev.quantumlab.physics;

import dev.quantumlab.math.ComplexVector;
import java.util.random.RandomGenerator;

/**
 * Projective (von Neumann) measurement of a state in an orthonormal basis.
 *
 * <p>Outcome {@code k} occurs with the Born probability {@code pₖ = |⟨bₖ|ψ⟩|²}; afterwards the
 * state is {@code |bₖ⟩} (up to a global phase). The outcome of <em>one</em> measurement is a random
 * draw: the theory predicts the distribution, not the individual result. A seeded generator makes
 * the draw reproducible, which is a property of the pseudo-random generator and not a claim that
 * nature is deterministic.
 */
public final class ProjectiveMeasurement {

    private ProjectiveMeasurement() {}

    /**
     * One measurement outcome.
     *
     * @param outcomeIndex which basis vector was found
     * @param outcomeLabel that vector's display label
     * @param probability the Born probability this outcome had before the draw
     * @param preState the state that was measured
     * @param postState the state immediately afterwards ({@code |bₖ⟩}, up to a global phase)
     */
    public record MeasurementResult(
            int outcomeIndex,
            String outcomeLabel,
            double probability,
            ComplexVector preState,
            ComplexVector postState) {}

    /** Measures {@code state} in {@code basis}, drawing the outcome from {@code rng}. */
    public static MeasurementResult measure(
            ComplexVector state, MeasurementBasis basis, RandomGenerator rng) {
        double[] p = BornRule.probabilities(state, basis);
        int k = sampleIndex(p, rng);
        return new MeasurementResult(k, basis.label(k), p[k], state, basis.vector(k));
    }

    public static MeasurementResult measure(
            QubitState state, MeasurementBasis basis, RandomGenerator rng) {
        return measure(state.vector(), basis, rng);
    }

    /**
     * Draws an index from the discrete distribution {@code p} by inverting its cumulative sum with
     * one uniform variate {@code u ∈ [0, 1)}: the first {@code k} with {@code u < p₀ + … + pₖ}.
     *
     * <p>An outcome with probability exactly zero is never returned. If rounding leaves the final
     * cumulative sum a hair below {@code u}, the last outcome with non-zero probability is returned.
     *
     * @throws IllegalArgumentException if no entry of {@code p} is positive
     */
    public static int sampleIndex(double[] p, RandomGenerator rng) {
        double u = rng.nextDouble();
        double cumulative = 0.0;
        int lastPositive = -1;
        for (int k = 0; k < p.length; k++) {
            if (p[k] > 0.0) {
                lastPositive = k;
            }
            cumulative += p[k];
            if (u < cumulative) {
                return k;
            }
        }
        if (lastPositive < 0) {
            throw new IllegalArgumentException("Distribution has no outcome with positive probability");
        }
        return lastPositive;
    }
}
