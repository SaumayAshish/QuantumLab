package dev.quantumlab.physics;

import dev.quantumlab.math.ComplexVector;
import dev.quantumlab.math.Tolerances;

/**
 * The Born rule: for a normalised state {@code |ψ⟩} and an orthonormal measurement basis {@code
 * {|bₖ⟩}}, the probability of outcome {@code k} is {@code pₖ = |⟨bₖ|ψ⟩|²}.
 *
 * <p>These are the <em>only</em> probabilities the simulator uses, and they are computed from the
 * amplitudes directly. Nothing here is a classical mixture: the probabilities depend on the relative
 * phase of the amplitudes whenever the basis is not the computational one.
 */
public final class BornRule {

    private BornRule() {}

    /**
     * @return {@code p[k] = |⟨bₖ|ψ⟩|²} for each outcome {@code k}, summing to one
     * @throws IllegalArgumentException if the state is not normalised or the dimensions disagree
     */
    public static double[] probabilities(ComplexVector state, MeasurementBasis basis) {
        if (state.dimension() != basis.dimension()) {
            throw new IllegalArgumentException(
                    "State dimension " + state.dimension() + " does not match basis dimension "
                            + basis.dimension());
        }
        if (!state.isNormalized(Tolerances.NORMALISATION)) {
            throw new IllegalArgumentException(
                    "State is not normalised: norm^2 = " + state.normSquared());
        }
        double[] p = new double[basis.size()];
        double sum = 0.0;
        for (int k = 0; k < p.length; k++) {
            p[k] = basis.vector(k).innerProduct(state).abs2();
            sum += p[k];
        }
        // Completeness of an orthonormal basis guarantees sum = ||psi||^2 = 1; check, don't assume.
        if (Math.abs(sum - 1.0) > Tolerances.NORMALISATION) {
            throw new IllegalStateException("Born probabilities sum to " + sum + ", expected 1");
        }
        return p;
    }

    public static double[] probabilities(QubitState state, MeasurementBasis basis) {
        return probabilities(state.vector(), basis);
    }
}
