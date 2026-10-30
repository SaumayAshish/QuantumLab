package dev.quantumlab.physics.composite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.quantumlab.math.Complex;
import dev.quantumlab.physics.MeasurementBasis;
import dev.quantumlab.physics.QubitOperators;
import dev.quantumlab.physics.QubitState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClassicalCorrelationTest {

    private static final double TOL = 1e-12;
    private static final MeasurementBasis Z = MeasurementBasis.computational();
    private static final MeasurementBasis X = MeasurementBasis.hadamard();
    private static final MeasurementBasis Y = MeasurementBasis.circular();

    @Test
    @DisplayName("p0 must be a probability")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> new ClassicalCorrelation(-0.1));
        assertThrows(IllegalArgumentException.class, () -> new ClassicalCorrelation(1.1));
        assertThrows(IllegalArgumentException.class, () -> new ClassicalCorrelation(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new ClassicalCorrelation(0.5).jointProbabilities(null, Z));
    }

    @Test
    @DisplayName("in the computational basis the mixture is identical to the entangled state")
    void sameAsEntangledInZ() {
        QubitState n = QubitState.of(Complex.of(0.8, 0), Complex.of(0.6, 0));
        double[][] mix = new ClassicalCorrelation(0.64).jointProbabilities(Z, Z);
        double[][] ent = TwoQubitState.correlated(n).jointProbabilities(Z, Z);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                assertEquals(ent[i][j], mix[i][j], TOL);
            }
        }
    }

    @Test
    @DisplayName("in the Hadamard and circular bases the mixture is uniformly random, unlike the entangled state")
    void differsInOtherBases() {
        ClassicalCorrelation mix = new ClassicalCorrelation(0.5);
        for (MeasurementBasis b : new MeasurementBasis[] {X, Y}) {
            double[][] p = mix.jointProbabilities(b, b);
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    assertEquals(0.25, p[i][j], TOL);
                }
            }
        }
        double[][] ent = TwoQubitState.correlated(QubitState.plus()).jointProbabilities(X, X);
        assertEquals(0.5, ent[0][0], TOL);
    }

    @Test
    @DisplayName("probabilities sum to 1 and the expectation matches the probabilities")
    void normalisedAndConsistent() {
        ClassicalCorrelation mix = new ClassicalCorrelation(0.3);
        double[][] p = mix.jointProbabilities(X, Y);
        assertEquals(1.0, p[0][0] + p[0][1] + p[1][0] + p[1][1], TOL);
        double[][] z = mix.jointProbabilities(Z, Z);
        double viaP = z[0][0] - z[0][1] - z[1][0] + z[1][1];
        assertEquals(viaP, mix.expectation(QubitOperators.PAULI_Z, QubitOperators.PAULI_Z), TOL);
        assertEquals(0.0, mix.expectation(QubitOperators.PAULI_X, QubitOperators.PAULI_X), TOL);
    }
}
