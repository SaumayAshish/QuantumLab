package dev.quantumlab.physics.decoherence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexMatrix;
import dev.quantumlab.physics.BornRule;
import dev.quantumlab.physics.MeasurementBasis;
import dev.quantumlab.physics.QubitState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DensityMatrixTest {

    private static final double TOL = 1e-12;
    private static final MeasurementBasis Z = MeasurementBasis.computational();
    private static final MeasurementBasis X = MeasurementBasis.hadamard();
    private static final MeasurementBasis Y = MeasurementBasis.circular();

    /** 0.6|0⟩ + 0.8i|1⟩, so that the coherence is −0.48i. */
    private static QubitState state() {
        return QubitState.of(Complex.of(0.6), Complex.of(0.0, 0.8));
    }

    @Test
    @DisplayName("a pure state has populations |α|², |β|² and coherence αβ*")
    void fromState() {
        DensityMatrix rho = DensityMatrix.fromState(state());
        assertEquals(0.36, rho.population0(), TOL);
        assertEquals(0.64, rho.population1(), TOL);
        assertEquals(0.0, rho.coherence().re(), TOL);
        assertEquals(-0.48, rho.coherence().im(), TOL);
        assertEquals(0.48, rho.coherenceMagnitude(), TOL);
    }

    @Test
    @DisplayName("a pure state has purity 1, Bloch length 1 and zero entropy")
    void pureState() {
        DensityMatrix rho = DensityMatrix.fromState(state());
        assertEquals(1.0, rho.purity(), TOL);
        assertEquals(1.0, rho.blochLength(), TOL);
        assertEquals(0.0, rho.entropyBits(), 1e-7);
    }

    @Test
    @DisplayName("the maximally mixed state has purity 1/2, Bloch length 0 and one bit of entropy")
    void maximallyMixed() {
        DensityMatrix rho = DensityMatrix.maximallyMixed();
        assertEquals(0.5, rho.purity(), TOL);
        assertEquals(0.0, rho.blochLength(), TOL);
        assertEquals(1.0, rho.entropyBits(), TOL);
        assertEquals(0.0, rho.fringeVisibility(), TOL);
    }

    @Test
    @DisplayName("Bloch components of |+i⟩ are (0, 1, 0) and of |1⟩ are (0, 0, −1)")
    void blochComponents() {
        DensityMatrix plusI = DensityMatrix.fromState(QubitState.plusI());
        assertEquals(0.0, plusI.blochX(), TOL);
        assertEquals(1.0, plusI.blochY(), TOL);
        assertEquals(0.0, plusI.blochZ(), TOL);
        DensityMatrix one = DensityMatrix.fromState(QubitState.basis1());
        assertEquals(-1.0, one.blochZ(), TOL);
        DensityMatrix plus = DensityMatrix.fromState(QubitState.plus());
        assertEquals(1.0, plus.blochX(), TOL);
    }

    @Test
    @DisplayName("probabilities for a pure state agree with the state-vector Born rule in Z, X and Y")
    void agreesWithBornRule() {
        QubitState s = state();
        DensityMatrix rho = DensityMatrix.fromState(s);
        for (MeasurementBasis basis : new MeasurementBasis[] {Z, X, Y}) {
            assertArrayEquals(BornRule.probabilities(s, basis), rho.probabilities(basis), TOL);
        }
    }

    @Test
    @DisplayName("the equatorial probability matches the Born rule in a basis built at that angle")
    void equatorialProbability() {
        DensityMatrix rho = DensityMatrix.fromState(state());
        double phase = 0.7;
        dev.quantumlab.math.ComplexVector plus =
            dev.quantumlab.math.ComplexVector.of(Complex.of(1.0), Complex.cis(phase)).scale(1.0 / Math.sqrt(2.0));
        dev.quantumlab.math.ComplexVector minus =
            dev.quantumlab.math.ComplexVector.of(Complex.of(1.0), Complex.cis(phase + Math.PI))
                .scale(1.0 / Math.sqrt(2.0));
        MeasurementBasis basis =
            new MeasurementBasis("equatorial", java.util.List.of("+", "−"), java.util.List.of(plus, minus));
        assertEquals(rho.probabilities(basis)[0], rho.equatorialProbability(phase), TOL);
        assertEquals(0.5 + 0.0, DensityMatrix.maximallyMixed().equatorialProbability(1.234), TOL);
    }

    @Test
    @DisplayName("the fringe visibility is 2|ρ01|, and the fringe height really is Pmax − Pmin")
    void fringeVisibility() {
        DensityMatrix rho = DensityMatrix.fromState(state());
        double max = 0.0;
        double min = 1.0;
        for (int k = 0; k < 3600; k++) {
            double p = rho.equatorialProbability(2.0 * Math.PI * k / 3600.0);
            max = Math.max(max, p);
            min = Math.min(min, p);
        }
        assertEquals(0.96, rho.fringeVisibility(), TOL);
        assertEquals((max - min) / (max + min), rho.fringeVisibility(), 1e-6);
    }

    @Test
    @DisplayName("a matrix that is not a density matrix is rejected")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> DensityMatrix.of(ComplexMatrix.identity(3)));
        assertThrows(IllegalArgumentException.class, () -> DensityMatrix.of(null));
        // trace 2
        assertThrows(IllegalArgumentException.class, () -> DensityMatrix.of(ComplexMatrix.identity(2)));
        // not Hermitian
        Complex[][] notHermitian = {{Complex.of(0.5), Complex.of(0.1)}, {Complex.of(0.2), Complex.of(0.5)}};
        assertThrows(IllegalArgumentException.class, () -> DensityMatrix.of(ComplexMatrix.of(notHermitian)));
        // a negative eigenvalue: coherence larger than the populations allow
        Complex[][] tooCoherent = {{Complex.of(0.5), Complex.of(0.6)}, {Complex.of(0.6), Complex.of(0.5)}};
        assertThrows(IllegalArgumentException.class, () -> DensityMatrix.of(ComplexMatrix.of(tooCoherent)));
        // negative population
        Complex[][] negative = {{Complex.of(1.5), Complex.ZERO}, {Complex.ZERO, Complex.of(-0.5)}};
        assertThrows(IllegalArgumentException.class, () -> DensityMatrix.of(ComplexMatrix.of(negative)));
    }

    @Test
    @DisplayName("a pure state sits exactly on the boundary and is accepted")
    void boundaryAccepted() {
        Complex[][] plus = {{Complex.of(0.5), Complex.of(0.5)}, {Complex.of(0.5), Complex.of(0.5)}};
        DensityMatrix rho = DensityMatrix.of(ComplexMatrix.of(plus));
        assertEquals(1.0, rho.purity(), TOL);
    }

    @Test
    @DisplayName("probabilities rejects a missing or non-qubit basis")
    void probabilitiesValidation() {
        DensityMatrix rho = DensityMatrix.maximallyMixed();
        assertThrows(IllegalArgumentException.class, () -> rho.probabilities(null));
    }
}
