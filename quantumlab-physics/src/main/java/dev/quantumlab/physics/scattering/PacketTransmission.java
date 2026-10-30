package dev.quantumlab.physics.scattering;

/**
 * The transmission probability of a Gaussian wave packet predicted from stationary scattering: the
 * transmission {@code T(E)} of a single energy, averaged over the packet's momentum distribution.
 *
 * <p>A Gaussian packet with position spread {@code σ} and mean wave number {@code k0} has
 * {@code |φ(k)|² ∝ exp(−2σ²(k − k0)²)}. Long after the collision the fraction found beyond the
 * barrier is {@code ∫ |φ(k)|² T(k²/2) dk} (ħ = m = 1). This is a prediction made without
 * simulating any motion, so it is an independent check on a time-dependent calculation.
 */
public final class PacketTransmission {

    /** Below this product {@code k0·σ} a visible part of the packet would move to the left. */
    public static final double MIN_WAVE_NUMBER_TIMES_WIDTH = 2.5;

    private static final int INTERVALS = 20_000;

    private PacketTransmission() {}

    public static double average(RectangularBarrier barrier, double sigma, double k0) {
        if (barrier == null) {
            throw new IllegalArgumentException("The barrier is required.");
        }
        if (!Double.isFinite(sigma) || sigma <= 0.0 || !Double.isFinite(k0) || k0 <= 0.0) {
            throw new IllegalArgumentException("The width and wave number must be positive and finite.");
        }
        if (k0 * sigma < MIN_WAVE_NUMBER_TIMES_WIDTH) {
            throw new IllegalArgumentException(
                "k0·σ must be at least " + MIN_WAVE_NUMBER_TIMES_WIDTH
                    + ": otherwise part of the packet moves away from the barrier.");
        }
        double halfRange = 4.0 / sigma; // eight standard deviations of the momentum distribution
        // k0·σ ≥ 2.5 means the weight at k ≤ 0 is below e^(−12.5), so the range may start just above 0.
        double lo = Math.max(k0 - halfRange, 1e-6);
        double hi = k0 + halfRange;
        double weighted = 0.0;
        double total = 0.0;
        for (int i = 0; i <= INTERVALS; i++) {
            double k = lo + (hi - lo) * i / INTERVALS;
            double w = Math.exp(-2.0 * sigma * sigma * (k - k0) * (k - k0));
            double edge = i == 0 || i == INTERVALS ? 0.5 : 1.0;
            weighted += edge * w * barrier.transmission(0.5 * k * k);
            total += edge * w;
        }
        return weighted / total;
    }
}
