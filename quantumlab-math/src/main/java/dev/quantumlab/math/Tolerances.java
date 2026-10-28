package dev.quantumlab.math;

/**
 * Numerical tolerances used across QuantumLab. Centralised so that "close enough" means the same
 * thing in every module and every test.
 *
 * <p>All values are absolute tolerances on dimensionless quantities (amplitudes, probabilities,
 * matrix entries).
 */
public final class Tolerances {

    /**
     * For identities that hold to a few ulps in double precision for small dimensions, such as
     * {@code (AB)† = B†A†} or {@code i·i = -1}.
     */
    public static final double STRICT = 1e-12;

    /**
     * For checking that a state vector is normalised, that a basis is orthonormal, and that Born
     * probabilities sum to one. Loose enough to absorb accumulated rounding, tight enough to catch
     * a genuinely un-normalised input.
     */
    public static final double NORMALISATION = 1e-9;

    private Tolerances() {}
}
