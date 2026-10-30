package dev.quantumlab.physics.composite;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexMatrix;
import dev.quantumlab.math.ComplexVector;
import dev.quantumlab.math.Tolerances;
import dev.quantumlab.physics.MeasurementBasis;
import dev.quantumlab.physics.QubitState;

/**
 * A pure state of two qubits: a normalised vector in {@code C⁴} with amplitudes ordered
 * {@code |00⟩, |01⟩, |10⟩, |11⟩}, the first label being the first qubit.
 *
 * <p>In the thought experiment the first qubit stands for the nucleus ({@code |0⟩} not decayed,
 * {@code |1⟩} decayed) and the second for an idealised detector ({@code |0⟩} no count, {@code |1⟩}
 * count). The second qubit is a two-level stand-in, not a model of a real detector.
 *
 * <p>The entanglement measures are those of a <em>pure</em> two-qubit state: the concurrence
 * {@code C = 2|a₀₀a₁₁ − a₀₁a₁₀|} and the entropy of either qubit's reduced state. Instances are
 * immutable.
 */
public final class TwoQubitState {

    private final ComplexVector vector;

    private TwoQubitState(ComplexVector vector) {
        this.vector = vector;
    }

    /** A state from four amplitudes; the vector must be normalised. */
    public static TwoQubitState of(ComplexVector v) {
        if (v == null || v.dimension() != 4) {
            throw new IllegalArgumentException("A two-qubit state needs a vector of dimension 4.");
        }
        if (!v.isNormalized(Tolerances.NORMALISATION)) {
            throw new IllegalArgumentException("The state is not normalised: norm² = " + v.normSquared());
        }
        return new TwoQubitState(v);
    }

    /** The unentangled state {@code |a⟩ ⊗ |b⟩}. */
    public static TwoQubitState product(QubitState a, QubitState b) {
        return new TwoQubitState(a.vector().tensor(b.vector()));
    }

    /**
     * The state {@code α|00⟩ + β|11⟩} in which the second qubit is a perfect copy, in the
     * computational basis, of the first: the idealised result of a detector reading the nucleus.
     * Entangled unless α or β is zero.
     */
    public static TwoQubitState correlated(QubitState nucleus) {
        return new TwoQubitState(
            ComplexVector.of(nucleus.alpha(), Complex.ZERO, Complex.ZERO, nucleus.beta()));
    }

    public ComplexVector vector() {
        return vector;
    }

    /** The amplitude of {@code |first, second⟩}, each label 0 or 1. */
    public Complex amplitude(int first, int second) {
        if (first < 0 || first > 1 || second < 0 || second > 1) {
            throw new IllegalArgumentException("Labels must be 0 or 1.");
        }
        return vector.get(2 * first + second);
    }

    /**
     * {@code p[i][j] = |⟨bᵢ ⊗ cⱼ|ψ⟩|²}: the probability that measuring the first qubit in
     * {@code first} gives outcome {@code i} and the second in {@code second} gives {@code j}.
     */
    public double[][] jointProbabilities(MeasurementBasis first, MeasurementBasis second) {
        requireQubitBasis(first);
        requireQubitBasis(second);
        double[][] p = new double[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                p[i][j] = first.vector(i).tensor(second.vector(j)).innerProduct(vector).abs2();
            }
        }
        return p;
    }

    /** The state of the first qubit alone: {@code ρ₁ = Tr₂ |ψ⟩⟨ψ|}. */
    public ComplexMatrix reducedFirst() {
        Complex[][] rho = new Complex[2][2];
        for (int i = 0; i < 2; i++) {
            for (int k = 0; k < 2; k++) {
                Complex sum = Complex.ZERO;
                for (int j = 0; j < 2; j++) {
                    sum = sum.plus(amplitude(i, j).times(amplitude(k, j).conjugate()));
                }
                rho[i][k] = sum;
            }
        }
        return ComplexMatrix.of(rho);
    }

    /** The state of the second qubit alone: {@code ρ₂ = Tr₁ |ψ⟩⟨ψ|}. */
    public ComplexMatrix reducedSecond() {
        Complex[][] rho = new Complex[2][2];
        for (int j = 0; j < 2; j++) {
            for (int l = 0; l < 2; l++) {
                Complex sum = Complex.ZERO;
                for (int i = 0; i < 2; i++) {
                    sum = sum.plus(amplitude(i, j).times(amplitude(i, l).conjugate()));
                }
                rho[j][l] = sum;
            }
        }
        return ComplexMatrix.of(rho);
    }

    /** {@code C = 2|a₀₀a₁₁ − a₀₁a₁₀|}: 0 for a product state, 1 for a maximally entangled one. */
    public double concurrence() {
        Complex det = amplitude(0, 0).times(amplitude(1, 1)).minus(amplitude(0, 1).times(amplitude(1, 0)));
        return Math.min(1.0, 2.0 * det.abs());
    }

    /** True if the state is, to within {@code tol} in the concurrence, a product {@code |a⟩⊗|b⟩}. */
    public boolean isProduct(double tol) {
        return concurrence() <= tol;
    }

    /** The purity {@code Tr ρ₁² = 1 − C²/2} of either qubit alone: 1 for a product state, 1/2 for a maximally entangled one. */
    public double reducedPurity() {
        double c = concurrence();
        return 1.0 - 0.5 * c * c;
    }

    /**
     * The entropy in bits of either qubit's reduced state, {@code −Σ λ log₂ λ} with
     * {@code λ = (1 ± √(1 − C²))/2}: 0 for a product state and 1 for a maximally entangled one.
     */
    public double entanglementEntropy() {
        double c = concurrence();
        double root = Math.sqrt(Math.max(0.0, 1.0 - c * c));
        return bits((1.0 + root) / 2.0) + bits((1.0 - root) / 2.0);
    }

    /** {@code ⟨ψ|A ⊗ B|ψ⟩} for two 2×2 Hermitian observables. */
    public double expectation(ComplexMatrix a, ComplexMatrix b) {
        if (a.rows() != 2 || a.cols() != 2 || b.rows() != 2 || b.cols() != 2) {
            throw new IllegalArgumentException("Both observables must be 2×2 matrices.");
        }
        return vector.innerProduct(a.kronecker(b).apply(vector)).re();
    }

    private static double bits(double p) {
        return p <= 0.0 ? 0.0 : -p * Math.log(p) / Math.log(2.0);
    }

    private static void requireQubitBasis(MeasurementBasis basis) {
        if (basis == null || basis.dimension() != 2) {
            throw new IllegalArgumentException("A basis for one qubit has dimension 2.");
        }
    }
}
