package dev.quantumlab.physics.decoherence;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexMatrix;
import dev.quantumlab.physics.QubitOperators;

/**
 * Pure dephasing of a qubit at a constant rate {@code γ}: the populations stay as they are, and the
 * coherence decays as {@code ρ₀₁(t) = ρ₀₁(0) e^{−γt}}.
 *
 * <p>This is a deliberately simple, <em>phenomenological</em> model. It is the standard
 * single-parameter description of a qubit that is slowly entangling with an unobserved environment,
 * but the rate {@code γ} is an input, not something derived from a physical environment. It does not
 * explain why one outcome is observed, and it does not model a cat.
 *
 * <p>Equivalently, with probability {@code q = (1 − e^{−γt})/2} the environment applies a Z flip:
 * {@code ρ → (1 − q) ρ + q ZρZ}. {@link #applyByRandomFlips} implements that route so the two
 * descriptions can be checked against each other.
 */
public final class Dephasing {

    private final double rate;

    /** @param rate the dephasing rate {@code γ ≥ 0}, in inverse time units */
    public Dephasing(double rate) {
        if (!Double.isFinite(rate) || rate < 0.0) {
            throw new IllegalArgumentException("The dephasing rate must be a finite number, 0 or more, got " + rate);
        }
        this.rate = rate;
    }

    public double rate() {
        return rate;
    }

    /** {@code e^{−γt}}: the fraction of the original coherence left after time {@code t}. */
    public double coherenceFactor(double t) {
        requireTime(t);
        return Math.exp(-rate * t);
    }

    /** {@code 1/γ}, the time for the coherence to fall to {@code 1/e}; infinite when there is no dephasing. */
    public double coherenceTime() {
        return rate == 0.0 ? Double.POSITIVE_INFINITY : 1.0 / rate;
    }

    /** The time at which the coherence has fallen to {@code fraction} of its starting value. */
    public double timeToFraction(double fraction) {
        if (!(fraction > 0.0) || fraction > 1.0) {
            throw new IllegalArgumentException("The fraction must be above 0 and at most 1, got " + fraction);
        }
        if (rate == 0.0) {
            if (fraction == 1.0) {
                return 0.0;
            }
            throw new IllegalArgumentException("With a dephasing rate of 0 the coherence never decays.");
        }
        return -Math.log(fraction) / rate;
    }

    /** The state after time {@code t}: the off-diagonal entries are multiplied by {@code e^{−γt}}. */
    public DensityMatrix apply(DensityMatrix rho, double t) {
        double f = coherenceFactor(t);
        ComplexMatrix m = rho.matrix();
        Complex[][] e = {
            {m.get(0, 0), m.get(0, 1).times(f)},
            {m.get(1, 0).times(f), m.get(1, 1)}
        };
        return DensityMatrix.of(ComplexMatrix.of(e));
    }

    /** The same evolution written as random Z flips, {@code (1 − q) ρ + q ZρZ}, with {@code q = (1 − e^{−γt})/2}. */
    public DensityMatrix applyByRandomFlips(DensityMatrix rho, double t) {
        double q = (1.0 - coherenceFactor(t)) / 2.0;
        ComplexMatrix z = QubitOperators.PAULI_Z;
        ComplexMatrix flipped = z.multiply(rho.matrix()).multiply(z);
        ComplexMatrix result =
            rho.matrix().scale(Complex.of(1.0 - q)).plus(flipped.scale(Complex.of(q)));
        return DensityMatrix.of(result);
    }

    /** The limit {@code t → ∞}: the coherence is gone and only the populations remain. */
    public static DensityMatrix fullyDephased(DensityMatrix rho) {
        Complex[][] e = {
            {rho.matrix().get(0, 0), Complex.ZERO},
            {Complex.ZERO, rho.matrix().get(1, 1)}
        };
        return DensityMatrix.of(ComplexMatrix.of(e));
    }

    private static void requireTime(double t) {
        if (!Double.isFinite(t) || t < 0.0) {
            throw new IllegalArgumentException("The time must be a finite number, 0 or more, got " + t);
        }
    }
}
