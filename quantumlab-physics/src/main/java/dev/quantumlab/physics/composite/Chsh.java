package dev.quantumlab.physics.composite;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexMatrix;
import dev.quantumlab.physics.QubitOperators;

/**
 * The CHSH quantity {@code S = E(a,b) + E(a,b′) + E(a′,b) − E(a′,b′)}, where {@code E} is the
 * correlation of two ±1-valued measurements, one on each qubit.
 *
 * <p>If each qubit's outcome is fixed in advance by a shared classical variable (a local
 * hidden-variable model) then {@code |S| ≤ 2}. Quantum mechanics allows up to {@code 2√2}
 * (Tsirelson's bound). A value above 2 therefore cannot come from ignorance about a classical
 * property. This is a statement about correlations between two qubits, not about cats.
 *
 * <p>A measurement setting here is an angle {@code θ}: the observable
 * {@code cos θ · Z + sin θ · X}, a spin measured along a direction in the x–z plane.
 */
public final class Chsh {

    public static final double CLASSICAL_BOUND = 2.0;
    public static final double TSIRELSON_BOUND = 2.0 * Math.sqrt(2.0);

    /** Anything that can report a correlation {@code E} for two observables. */
    @FunctionalInterface
    public interface Correlator {
        double correlation(ComplexMatrix first, ComplexMatrix second);
    }

    private Chsh() {}

    /** {@code cos θ · Z + sin θ · X}, a Hermitian matrix with eigenvalues ±1. */
    public static ComplexMatrix observable(double theta) {
        return observable(theta, 0.0);
    }

    /**
     * {@code cos θ · Z + sin θ · (cos φ · X + sin φ · Y)}: a spin measured along the direction with
     * polar angle {@code θ} from the z axis and azimuth {@code φ}. Hermitian, eigenvalues ±1.
     */
    public static ComplexMatrix observable(double theta, double azimuth) {
        if (!Double.isFinite(theta) || !Double.isFinite(azimuth)) {
            throw new IllegalArgumentException("The angles must be finite.");
        }
        double sx = Math.sin(theta) * Math.cos(azimuth);
        double sy = Math.sin(theta) * Math.sin(azimuth);
        return QubitOperators.PAULI_Z.scale(Complex.of(Math.cos(theta)))
            .plus(QubitOperators.PAULI_X.scale(Complex.of(sx)))
            .plus(QubitOperators.PAULI_Y.scale(Complex.of(sy)));
    }

    /** {@code S} for the four settings {@code a, a′} (first qubit) and {@code b, b′} (second qubit). */
    public static double value(Correlator state, double a, double a2, double b, double b2) {
        return e(state, a, b) + e(state, a, b2) + e(state, a2, b) - e(state, a2, b2);
    }

    /**
     * The settings that maximise {@code S} for a state with {@code ⟨Z⊗Z⟩ = 1} and
     * {@code ⟨X⊗X⟩ = c}, such as {@code α|00⟩ + β|11⟩} with {@code c = 2 Re(α*β)}:
     * {@code a = 0, a′ = π/2, b = atan c, b′ = −atan c}, in that order.
     */
    public static double[] optimalSettings(double c) {
        if (!Double.isFinite(c)) {
            throw new IllegalArgumentException("c must be finite.");
        }
        double t = Math.atan(c);
        return new double[] {0.0, Math.PI / 2.0, t, -t};
    }

    /**
     * {@code 2√(1 + c²)}: the largest {@code S} reachable with settings in the x–z plane when
     * {@code ⟨X⊗X⟩ = c}. For a pure two-qubit state of concurrence {@code C} it is also the largest
     * {@code S} over <em>all</em> settings, with {@code c = C}.
     */
    public static double maximumInPlane(double c) {
        return 2.0 * Math.sqrt(1.0 + c * c);
    }

    /** {@code S} for four given observables, which may point in any directions. */
    public static double value(
        Correlator state, ComplexMatrix a, ComplexMatrix a2, ComplexMatrix b, ComplexMatrix b2) {
        return state.correlation(a, b)
            + state.correlation(a, b2)
            + state.correlation(a2, b)
            - state.correlation(a2, b2);
    }

    private static double e(Correlator state, double thetaFirst, double thetaSecond) {
        return state.correlation(observable(thetaFirst), observable(thetaSecond));
    }
}
