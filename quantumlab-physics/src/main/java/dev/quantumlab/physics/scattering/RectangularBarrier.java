package dev.quantumlab.physics.scattering;

import java.util.List;

/**
 * A single rectangular potential barrier of height {@code V0} and width {@code a} (ħ = m = 1), with
 * closed-form transmission to compare against the transfer-matrix result.
 *
 * <p>For {@code E < V0}, with {@code κ = √(2(V0 − E))}:
 * {@code T = 1 / (1 + V0² sinh²(κa) / (4E(V0 − E)))}. For {@code E > V0}, with
 * {@code k = √(2(E − V0))}: {@code T = 1 / (1 + V0² sin²(ka) / (4E(E − V0)))}. At {@code E = V0}:
 * {@code T = 1 / (1 + a² V0 / 2)}. Transmission through a classically forbidden barrier
 * ({@code E < V0}) is quantum tunnelling.
 *
 * @param height the barrier height V0, positive and finite
 * @param width the barrier width a, positive and finite
 */
public record RectangularBarrier(double height, double width) {

    public RectangularBarrier {
        if (!Double.isFinite(height) || height <= 0.0) {
            throw new IllegalArgumentException("The barrier height must be positive and finite.");
        }
        if (!Double.isFinite(width) || width <= 0.0) {
            throw new IllegalArgumentException("The barrier width must be positive and finite.");
        }
    }

    /** Closed-form transmission probability. */
    public double transmission(double energy) {
        requireEnergy(energy);
        double v = height;
        double a = width;
        if (energy < v) {
            double kappa = Math.sqrt(2.0 * (v - energy));
            double s = Math.sinh(kappa * a);
            return 1.0 / (1.0 + v * v * s * s / (4.0 * energy * (v - energy)));
        }
        if (energy > v) {
            double k = Math.sqrt(2.0 * (energy - v));
            double s = Math.sin(k * a);
            return 1.0 / (1.0 + v * v * s * s / (4.0 * energy * (energy - v)));
        }
        return 1.0 / (1.0 + a * a * v / 2.0);
    }

    /** The same probability from the transfer-matrix method; agrees with {@link #transmission}. */
    public double transmissionByTransferMatrix(double energy) {
        return Scattering.transmission(List.of(new Layer(height, width)), energy);
    }

    /**
     * The decay constant {@code κ = √(2(V0 − E))} of the wave inside the barrier, for {@code E < V0}.
     *
     * @throws IllegalArgumentException if the energy is not below the barrier height
     */
    public double decayConstant(double energy) {
        requireEnergy(energy);
        if (!(energy < height)) {
            throw new IllegalArgumentException("The energy must be below the barrier height.");
        }
        return Math.sqrt(2.0 * (height - energy));
    }

    /**
     * The thick-barrier estimate {@code 16 E (V0 − E) / V0² · e^(−2κa)}, valid for {@code κa ≫ 1}.
     *
     * @throws IllegalArgumentException if the energy is not below the barrier height
     */
    public double thickBarrierEstimate(double energy) {
        double kappa = decayConstant(energy);
        return 16.0 * energy * (height - energy) / (height * height) * Math.exp(-2.0 * kappa * width);
    }

    private static void requireEnergy(double energy) {
        if (!(energy > 0.0) || !Double.isFinite(energy)) {
            throw new IllegalArgumentException("The energy must be positive and finite.");
        }
    }
}
