package dev.quantumlab.physics;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexMatrix;

/**
 * Standard single-qubit operators in the computational basis {@code {|0⟩, |1⟩}} (row/column 0 is
 * {@code |0⟩}).
 */
public final class QubitOperators {

    private static final double INV_SQRT2 = 1.0 / Math.sqrt(2.0);

    /** {@code σₓ = [[0, 1], [1, 0]]}. */
    public static final ComplexMatrix PAULI_X =
            ComplexMatrix.of(
                    new Complex[][] {
                            {Complex.ZERO, Complex.ONE},
                            {Complex.ONE, Complex.ZERO}
                    });

    /** {@code σᵧ = [[0, −i], [i, 0]]}. */
    public static final ComplexMatrix PAULI_Y =
            ComplexMatrix.of(
                    new Complex[][] {
                            {Complex.ZERO, Complex.of(0, -1)},
                            {Complex.of(0, 1), Complex.ZERO}
                    });

    /** {@code σ_z = [[1, 0], [0, −1]]}. */
    public static final ComplexMatrix PAULI_Z =
            ComplexMatrix.of(
                    new Complex[][] {
                            {Complex.ONE, Complex.ZERO},
                            {Complex.ZERO, Complex.of(-1)}
                    });

    /** {@code H = (1/√2) [[1, 1], [1, −1]]}; maps the Z basis to the X basis and back. */
    public static final ComplexMatrix HADAMARD =
            ComplexMatrix.of(
                    new Complex[][] {
                            {Complex.of(INV_SQRT2), Complex.of(INV_SQRT2)},
                            {Complex.of(INV_SQRT2), Complex.of(-INV_SQRT2)}
                    });

    private QubitOperators() {}

    /** {@code diag(1, e^{iφ})}: leaves {@code |0⟩} alone and rotates the relative phase by {@code φ}. */
    public static ComplexMatrix phase(double phi) {
        return ComplexMatrix.of(
                new Complex[][] {
                        {Complex.ONE, Complex.ZERO},
                        {Complex.ZERO, Complex.cis(phi)}
                });
    }
}
