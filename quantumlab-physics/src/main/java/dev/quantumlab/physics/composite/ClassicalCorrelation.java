package dev.quantumlab.physics.composite;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexMatrix;
import dev.quantumlab.math.ComplexVector;
import dev.quantumlab.physics.MeasurementBasis;

/**
 * The classical alternative to {@link TwoQubitState#correlated}: ignorance, not superposition. The
 * nucleus has <em>either</em> not decayed (probability {@code p0}) <em>or</em> decayed, the detector
 * agrees with it, and we simply do not know which. The state is the mixture
 * {@code ρ = p0 |00⟩⟨00| + (1 − p0) |11⟩⟨11|}, which has no amplitudes to interfere.
 *
 * <p>Measured in the computational basis on both qubits it gives exactly the same statistics as
 * the entangled state with {@code |α|² = p0}. Measured in other bases it does not.
 */
public record ClassicalCorrelation(double p0) {

    public ClassicalCorrelation {
        if (!Double.isFinite(p0) || p0 < 0.0 || p0 > 1.0) {
            throw new IllegalArgumentException("p0 must be a probability between 0 and 1, got " + p0);
        }
    }

    /** {@code p[i][j]} for the two qubit bases, with the same meaning as for {@link TwoQubitState}. */
    public double[][] jointProbabilities(MeasurementBasis first, MeasurementBasis second) {
        requireQubitBasis(first);
        requireQubitBasis(second);
        double[][] p = new double[2][2];
        double p1 = 1.0 - p0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                p[i][j] =
                    p0 * first.vector(i).innerProduct(zero()).abs2() * second.vector(j).innerProduct(zero()).abs2()
                        + p1 * first.vector(i).innerProduct(one()).abs2()
                        * second.vector(j).innerProduct(one()).abs2();
            }
        }
        return p;
    }

    /** {@code Tr(ρ · A ⊗ B) = p0·A₀₀B₀₀ + (1 − p0)·A₁₁B₁₁} for two 2×2 observables. */
    public double expectation(ComplexMatrix a, ComplexMatrix b) {
        if (a.rows() != 2 || a.cols() != 2 || b.rows() != 2 || b.cols() != 2) {
            throw new IllegalArgumentException("Both observables must be 2×2 matrices.");
        }
        Complex first = a.get(0, 0).times(b.get(0, 0));
        Complex second = a.get(1, 1).times(b.get(1, 1));
        return p0 * first.re() + (1.0 - p0) * second.re();
    }

    private static ComplexVector zero() {
        return ComplexVector.basis(2, 0);
    }

    private static ComplexVector one() {
        return ComplexVector.basis(2, 1);
    }

    private static void requireQubitBasis(MeasurementBasis basis) {
        if (basis == null || basis.dimension() != 2) {
            throw new IllegalArgumentException("A basis for one qubit has dimension 2.");
        }
    }
}
