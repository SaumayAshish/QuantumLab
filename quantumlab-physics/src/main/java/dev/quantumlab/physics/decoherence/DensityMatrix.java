package dev.quantumlab.physics.decoherence;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexMatrix;
import dev.quantumlab.math.ComplexVector;
import dev.quantumlab.physics.MeasurementBasis;
import dev.quantumlab.physics.QubitState;

/**
 * The state of one qubit as a 2×2 density matrix {@code ρ}. Unlike a state vector, a density matrix
 * can describe a <em>mixture</em> of states, which is what a qubit becomes when it loses coherence.
 *
 * <p>The diagonal holds the probabilities of the computational outcomes ({@code ρ₀₀} for "not
 * decayed", {@code ρ₁₁} for "decayed"). The off-diagonal entry {@code ρ₀₁} is the <em>coherence</em>:
 * the part that lets the two outcomes interfere. A density matrix is Hermitian, has trace 1 and no
 * negative eigenvalues; the constructor checks all three.
 */
public final class DensityMatrix {

    /** How far from an exact Hermitian, trace-1, non-negative matrix an input may be. */
    static final double TOLERANCE = 1e-9;

    private final ComplexMatrix matrix;

    private DensityMatrix(ComplexMatrix matrix) {
        this.matrix = matrix;
    }

    /** Validates {@code m} as a density matrix. */
    public static DensityMatrix of(ComplexMatrix m) {
        if (m == null || m.rows() != 2 || m.cols() != 2) {
            throw new IllegalArgumentException("A qubit density matrix must be 2×2.");
        }
        if (!m.isHermitian(TOLERANCE)) {
            throw new IllegalArgumentException("A density matrix must be Hermitian.");
        }
        double r00 = m.get(0, 0).re();
        double r11 = m.get(1, 1).re();
        if (Math.abs(r00 + r11 - 1.0) > TOLERANCE) {
            throw new IllegalArgumentException("A density matrix must have trace 1, got " + (r00 + r11));
        }
        // With trace 1, a determinant that is not negative means both eigenvalues are not negative.
        double determinant = r00 * r11 - m.get(0, 1).abs2();
        if (determinant < -TOLERANCE) {
            throw new IllegalArgumentException("A density matrix must not have negative eigenvalues.");
        }
        return new DensityMatrix(m);
    }

    /** {@code ρ = |ψ⟩⟨ψ|}: the pure state with state vector {@code ψ}. */
    public static DensityMatrix fromState(QubitState state) {
        ComplexVector v = state.vector();
        Complex[][] e = new Complex[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                e[i][j] = v.get(i).times(v.get(j).conjugate());
            }
        }
        return new DensityMatrix(ComplexMatrix.of(e));
    }

    /** {@code ρ = I/2}: complete ignorance, no coherence. */
    public static DensityMatrix maximallyMixed() {
        return new DensityMatrix(ComplexMatrix.identity(2).scale(Complex.of(0.5)));
    }

    /** The 2×2 matrix itself. */
    public ComplexMatrix matrix() {
        return matrix;
    }

    /** {@code ρ₀₀}: probability of the first computational outcome. */
    public double population0() {
        return matrix.get(0, 0).re();
    }

    /** {@code ρ₁₁}: probability of the second computational outcome. */
    public double population1() {
        return matrix.get(1, 1).re();
    }

    /** {@code ρ₀₁}, the coherence between the two computational states. */
    public Complex coherence() {
        return matrix.get(0, 1);
    }

    /** {@code |ρ₀₁|}. */
    public double coherenceMagnitude() {
        return coherence().abs();
    }

    /** {@code Tr ρ² = ρ₀₀² + ρ₁₁² + 2|ρ₀₁|²}: 1 for a pure state, ½ for the maximally mixed one. */
    public double purity() {
        return population0() * population0()
            + population1() * population1()
            + 2.0 * coherence().abs2();
    }

    /** Bloch vector component {@code ⟨X⟩ = 2 Re ρ₀₁}. */
    public double blochX() {
        return 2.0 * coherence().re();
    }

    /** Bloch vector component {@code ⟨Y⟩ = −2 Im ρ₀₁}. */
    public double blochY() {
        return -2.0 * coherence().im();
    }

    /** Bloch vector component {@code ⟨Z⟩ = ρ₀₀ − ρ₁₁}. */
    public double blochZ() {
        return population0() - population1();
    }

    /** Length of the Bloch vector: 1 for a pure state, 0 for the maximally mixed one. */
    public double blochLength() {
        return Math.sqrt(blochX() * blochX() + blochY() * blochY() + blochZ() * blochZ());
    }

    /** Von Neumann entropy {@code −Tr ρ log₂ ρ} in bits: 0 for a pure state, 1 for {@code I/2}. */
    public double entropyBits() {
        double r = Math.min(1.0, blochLength());
        return term((1.0 + r) / 2.0) + term((1.0 - r) / 2.0);
    }

    private static double term(double lambda) {
        return lambda <= 0.0 ? 0.0 : -lambda * Math.log(lambda) / Math.log(2.0);
    }

    /** Born rule for a mixed state: {@code P(k) = ⟨bₖ|ρ|bₖ⟩}. */
    public double[] probabilities(MeasurementBasis basis) {
        if (basis == null || basis.dimension() != 2) {
            throw new IllegalArgumentException("The measurement basis must belong to a qubit.");
        }
        double[] p = new double[2];
        for (int k = 0; k < 2; k++) {
            ComplexVector b = basis.vector(k);
            p[k] = b.innerProduct(matrix.apply(b)).re();
        }
        return p;
    }

    /**
     * Probability of the outcome {@code (|0⟩ + e^{iφ}|1⟩)/√2} in an equatorial basis whose angle
     * {@code φ} is varied: {@code ½ + Re(ρ₀₁ e^{iφ})}. As {@code φ} varies this traces an
     * interference fringe whose height is set by the coherence.
     */
    public double equatorialProbability(double phase) {
        return 0.5 + coherence().times(Complex.cis(phase)).re();
    }

    /**
     * Visibility {@code (Pmax − Pmin)/(Pmax + Pmin)} of the equatorial fringe, which equals
     * {@code 2|ρ₀₁|}. It is 1 for an equal superposition and 0 once the coherence is gone.
     */
    public double fringeVisibility() {
        return 2.0 * coherenceMagnitude();
    }

    @Override
    public String toString() {
        return "DensityMatrix" + matrix;
    }
}
