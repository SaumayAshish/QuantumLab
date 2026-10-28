package dev.quantumlab.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexVector;
import dev.quantumlab.math.Tolerances;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeasurementBasisTest {

    private static final double TOL = Tolerances.STRICT;

    @Test
    @DisplayName("every standard basis is a complete orthonormal pair")
    void standardBasesAreOrthonormal() {
        for (StandardBasis sb : StandardBasis.values()) {
            MeasurementBasis b = sb.basis();
            assertEquals(2, b.size());
            assertEquals(2, b.dimension());
            assertEquals(1.0, b.vector(0).innerProduct(b.vector(0)).re(), TOL);
            assertEquals(1.0, b.vector(1).innerProduct(b.vector(1)).re(), TOL);
            assertEquals(0.0, b.vector(0).innerProduct(b.vector(1)).abs(), TOL);
        }
    }

    @Test
    @DisplayName("the Hadamard basis is the Hadamard gate applied to the computational basis")
    void hadamardBasisIsRotatedComputational() {
        MeasurementBasis x = MeasurementBasis.hadamard();
        for (int k = 0; k < 2; k++) {
            ComplexVector rotated = QubitOperators.HADAMARD.apply(ComplexVector.basis(2, k));
            assertTrue(x.vector(k).approxEquals(rotated, TOL));
        }
    }

    @Test
    @DisplayName("the circular basis is |0> +/- i|1> over sqrt(2)")
    void circularBasisVectors() {
        double s = 1.0 / Math.sqrt(2.0);
        MeasurementBasis y = MeasurementBasis.circular();
        assertTrue(y.vector(0).approxEquals(ComplexVector.of(Complex.of(s), Complex.of(0, s)), TOL));
        assertTrue(
                y.vector(1).approxEquals(ComplexVector.of(Complex.of(s), Complex.of(0, -s)), TOL));
    }

    @Test
    @DisplayName("labels follow outcome order")
    void labels() {
        assertEquals("|0⟩", MeasurementBasis.computational().label(0));
        assertEquals("|1⟩", MeasurementBasis.computational().label(1));
        assertEquals("|+⟩", MeasurementBasis.hadamard().label(0));
        assertEquals("|−⟩", MeasurementBasis.hadamard().label(1));
    }

    @Test
    @DisplayName("a non-orthogonal pair is rejected")
    void rejectsNonOrthogonal() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MeasurementBasis(
                                "bad",
                                List.of("a", "b"),
                                List.of(
                                        ComplexVector.basis(2, 0),
                                        QubitState.plus().vector())));
    }

    @Test
    @DisplayName("un-normalised vectors are rejected")
    void rejectsNonNormalised() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MeasurementBasis(
                                "bad",
                                List.of("a", "b"),
                                List.of(
                                        ComplexVector.basis(2, 0).scale(2.0),
                                        ComplexVector.basis(2, 1))));
    }

    @Test
    @DisplayName("an incomplete basis is rejected")
    void rejectsIncomplete() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MeasurementBasis(
                                "bad", List.of("a"), List.of(ComplexVector.basis(2, 0))));
    }

    @Test
    @DisplayName("a label count that does not match the vector count is rejected")
    void rejectsLabelMismatch() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MeasurementBasis(
                                "bad",
                                List.of("only one"),
                                List.of(ComplexVector.basis(2, 0), ComplexVector.basis(2, 1))));
    }
}
