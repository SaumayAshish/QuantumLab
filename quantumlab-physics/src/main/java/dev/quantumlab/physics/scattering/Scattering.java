package dev.quantumlab.physics.scattering;

import java.util.List;

/**
 * Transmission of a plane wave through a stack of constant-potential slabs, by the transfer-matrix
 * method (ħ = m = 1). The potential is zero to the left and right of the stack, and the particle
 * arrives from the left with energy {@code E > 0}.
 *
 * <p>Inside a slab with potential {@code V} the state {@code (ψ, ψ')} is carried across a slab of
 * width {@code d} by a real 2×2 matrix of determinant 1: trigonometric functions when {@code E > V},
 * hyperbolic ones when {@code E < V}, and a shear when {@code E = V}. Multiplying the slab matrices
 * gives {@code M}, and with the outgoing wave on the right only,
 * {@code T = 4 / ((m11 + m22)² + (k m12 − m21 / k)²)}, {@code k = √(2E)}.
 *
 * <p>This is a stationary calculation: it gives the long-time transmission probability of a wave
 * of one definite energy, not the motion of a wave packet.
 */
public final class Scattering {

    private Scattering() {
    }

    /**
     * The transmission probability {@code T}, between 0 and 1.
     *
     * @param layers the slabs from left to right; may be empty, which transmits everything
     * @param energy the particle energy, positive and finite
     * @return the probability; values too small to represent (below about 1e-300) are returned as 0
     */
    public static double transmission(List<Layer> layers, double energy) {
        if (layers == null) {
            throw new IllegalArgumentException("The layers must not be null.");
        }
        if (!(energy > 0.0) || !Double.isFinite(energy)) {
            throw new IllegalArgumentException("The energy must be positive and finite.");
        }
        // (ψ, ψ') at the left edge = M · (ψ, ψ') at the right edge, M = L1 · L2 · … · Ln.
        double m11 = 1.0;
        double m12 = 0.0;
        double m21 = 0.0;
        double m22 = 1.0;
        for (Layer layer : layers) {
            double[] l = slab(layer.potential(), layer.width(), energy);
            double n11 = m11 * l[0] + m12 * l[2];
            double n12 = m11 * l[1] + m12 * l[3];
            double n21 = m21 * l[0] + m22 * l[2];
            double n22 = m21 * l[1] + m22 * l[3];
            m11 = n11;
            m12 = n12;
            m21 = n21;
            m22 = n22;
        }
        if (!Double.isFinite(m11 + m12 + m21 + m22)) {
            // The stack is so opaque that the matrix overflowed; the true T is far below the
            // smallest positive double, so it is reported as 0 rather than as NaN.
            return 0.0;
        }
        double k = Math.sqrt(2.0 * energy);
        double sum = m11 + m22;
        double diff = k * m12 - m21 / k;
        return 4.0 / (sum * sum + diff * diff);
    }

    /**
     * {@code 1 − T}: the probability of being reflected.
     */
    public static double reflection(List<Layer> layers, double energy) {
        return 1.0 - transmission(layers, energy);
    }

    /**
     * The matrix {@code [m11, m12, m21, m22]} of one slab.
     */
    private static double[] slab(double potential, double width, double energy) {
        double e = energy - potential;
        if (e > 0.0) {
            double k = Math.sqrt(2.0 * e);
            double c = Math.cos(k * width);
            double s = Math.sin(k * width);
            return new double[]{c, s / k, -k * s, c};
        }
        if (e < 0.0) {
            double kappa = Math.sqrt(-2.0 * e);
            double c = Math.cosh(kappa * width);
            double s = Math.sinh(kappa * width);
            return new double[]{c, s / kappa, kappa * s, c};
        }
        return new double[]{1.0, width, 0.0, 1.0};
    }
}
