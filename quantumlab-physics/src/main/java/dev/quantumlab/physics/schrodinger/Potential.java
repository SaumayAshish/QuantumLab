package dev.quantumlab.physics.schrodinger;

/**
 * A potential energy {@code V(x)} in natural units (ħ = 1, particle mass m = 1), so that the
 * Hamiltonian is {@code H = −½ d²/dx² + V(x)}.
 */
@FunctionalInterface
public interface Potential {

    double value(double x);

    /** {@code V = 0} everywhere: a free particle between the walls. */
    static Potential zero() {
        return x -> 0.0;
    }

    /** {@code V = v0} everywhere. */
    static Potential constant(double v0) {
        if (!Double.isFinite(v0)) {
            throw new IllegalArgumentException("The potential must be finite.");
        }
        return x -> v0;
    }

    /** The harmonic oscillator {@code V = ½ ω² x²}, whose exact levels are {@code ω(n + ½)}. */
    static Potential harmonic(double omega) {
        if (!Double.isFinite(omega) || omega <= 0.0) {
            throw new IllegalArgumentException("omega must be positive and finite.");
        }
        return x -> 0.5 * omega * omega * x * x;
    }
}
