package dev.quantumlab.physics;

/**
 * Exponential decay of a single unstable nucleus: the probability that it has decayed by time
 * {@code t} is {@code 1 − e^{−λt}}.
 *
 * <p>Time is dimensionless ("model time units"); {@code λ} has units of inverse model time. Nothing
 * here is tied to a real isotope.
 *
 * <p>This is the <em>classical statistical law</em> of radioactive decay. It says nothing about
 * coherence; whether the nucleus is described as a classical coin-flip or as a quantum
 * superposition is a modelling choice made elsewhere (see {@link QubitState#fromDecay}).
 *
 * @param decayRate the decay constant {@code λ ≥ 0}; zero means a stable nucleus
 */
public record DecayModel(double decayRate) {

    public DecayModel {
        if (!Double.isFinite(decayRate) || decayRate < 0.0) {
            throw new IllegalArgumentException(
                    "Decay rate must be finite and >= 0, got " + decayRate);
        }
    }

    /** The model whose half-life is {@code halfLife} (so {@code λ = ln 2 / halfLife}). */
    public static DecayModel fromHalfLife(double halfLife) {
        if (!Double.isFinite(halfLife) || halfLife <= 0.0) {
            throw new IllegalArgumentException("Half-life must be finite and > 0, got " + halfLife);
        }
        return new DecayModel(Math.log(2.0) / halfLife);
    }

    /** {@code P(not decayed by t) = e^{−λt}}. */
    public double survivalProbability(double t) {
        requireTime(t);
        return Math.exp(-decayRate * t);
    }

    /**
     * {@code P(decayed by t) = 1 − e^{−λt}}, computed as {@code −expm1(−λt)} so that it keeps full
     * relative precision when {@code λt} is tiny (the naive {@code 1 − exp(−x)} cancels
     * catastrophically).
     */
    public double decayProbability(double t) {
        requireTime(t);
        return -Math.expm1(-decayRate * t);
    }

    /** {@code t½ = ln 2 / λ}, or {@code +∞} for a stable nucleus. */
    public double halfLife() {
        return decayRate == 0.0 ? Double.POSITIVE_INFINITY : Math.log(2.0) / decayRate;
    }

    private static void requireTime(double t) {
        if (!Double.isFinite(t) || t < 0.0) {
            throw new IllegalArgumentException("Time must be finite and >= 0, got " + t);
        }
    }
}
