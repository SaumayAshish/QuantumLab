package dev.quantumlab.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexVector;
import dev.quantumlab.math.Tolerances;
import java.util.OptionalDouble;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QubitStateTest {

    private static final double TOL = Tolerances.STRICT;
    private static final double S = 1.0 / Math.sqrt(2.0);

    @Test
    @DisplayName("|0> and |1> are the basis states")
    void basisStates() {
        assertEquals(Complex.ONE, QubitState.basis0().alpha());
        assertEquals(Complex.ZERO, QubitState.basis0().beta());
        assertEquals(Complex.ZERO, QubitState.basis1().alpha());
        assertEquals(Complex.ONE, QubitState.basis1().beta());
        assertEquals(1.0, QubitState.basis0().probability0(), TOL);
        assertEquals(1.0, QubitState.basis1().probability1(), TOL);
    }

    @Test
    @DisplayName("named superpositions have the documented amplitudes")
    void namedSuperpositions() {
        assertTrue(QubitState.plus().alpha().approxEquals(Complex.of(S, 0), TOL));
        assertTrue(QubitState.plus().beta().approxEquals(Complex.of(S, 0), TOL));
        assertTrue(QubitState.minus().beta().approxEquals(Complex.of(-S, 0), TOL));
        assertTrue(QubitState.plusI().beta().approxEquals(Complex.of(0, S), TOL));
        assertTrue(QubitState.minusI().beta().approxEquals(Complex.of(0, -S), TOL));
    }

    @Test
    @DisplayName("all named states are normalised")
    void namedStatesNormalised() {
        QubitState[] all = {
                QubitState.basis0(), QubitState.basis1(), QubitState.plus(),
                QubitState.minus(), QubitState.plusI(), QubitState.minusI()
        };
        for (QubitState s : all) {
            assertEquals(1.0, s.normSquared(), TOL);
        }
    }

    @Test
    @DisplayName("an un-normalised state is rejected by of() but accepted by normalizing()")
    void normalisationEnforced() {
        assertThrows(
                IllegalArgumentException.class, () -> QubitState.of(Complex.ONE, Complex.ONE));
        QubitState s = QubitState.normalizing(Complex.ONE, Complex.ONE);
        assertTrue(s.sameRayAs(QubitState.plus(), TOL));
        assertEquals(1.0, s.normSquared(), TOL);
    }

    @Test
    @DisplayName("normalizing the zero state is rejected")
    void normalizingZero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> QubitState.normalizing(Complex.ZERO, Complex.ZERO));
    }

    @Test
    @DisplayName("fromVector requires dimension 2")
    void fromVectorDimension() {
        assertThrows(
                IllegalArgumentException.class,
                () -> QubitState.fromVector(ComplexVector.basis(3, 0)));
    }

    @Test
    @DisplayName("relative phase of the named states")
    void relativePhaseNamed() {
        assertEquals(0.0, QubitState.plus().relativePhase().getAsDouble(), TOL);
        assertEquals(Math.PI, QubitState.minus().relativePhase().getAsDouble(), TOL);
        assertEquals(Math.PI / 2, QubitState.plusI().relativePhase().getAsDouble(), TOL);
        assertEquals(-Math.PI / 2, QubitState.minusI().relativePhase().getAsDouble(), TOL);
    }

    @Test
    @DisplayName("relative phase is undefined when an amplitude vanishes")
    void relativePhaseUndefined() {
        assertEquals(OptionalDouble.empty(), QubitState.basis0().relativePhase());
        assertEquals(OptionalDouble.empty(), QubitState.basis1().relativePhase());
    }

    @Test
    @DisplayName("relative phase is wrapped into (-pi, pi]")
    void relativePhaseWrapped() {
        QubitState s = QubitState.of(Complex.of(S), Complex.cis(3.5).times(S));
        assertEquals(3.5 - 2 * Math.PI, s.relativePhase().getAsDouble(), TOL);
    }

    @Test
    @DisplayName("a global phase changes neither probabilities nor the relative phase")
    void globalPhaseInvariance() {
        QubitState base = QubitState.fromBloch(1.1, 0.6);
        Complex g = Complex.cis(2.3);
        QubitState shifted = QubitState.of(base.alpha().times(g), base.beta().times(g));

        assertEquals(base.probability0(), shifted.probability0(), TOL);
        assertEquals(base.probability1(), shifted.probability1(), TOL);
        assertEquals(
                base.relativePhase().getAsDouble(), shifted.relativePhase().getAsDouble(), TOL);
        assertTrue(base.sameRayAs(shifted, TOL));
        // ... although the amplitudes themselves did change.
        assertFalse(base.alpha().approxEquals(shifted.alpha(), 1e-3));
    }

    @Test
    @DisplayName("fromBloch reproduces the named states")
    void blochNamedStates() {
        assertTrue(QubitState.fromBloch(0, 0).sameRayAs(QubitState.basis0(), TOL));
        assertTrue(QubitState.fromBloch(Math.PI, 0).sameRayAs(QubitState.basis1(), TOL));
        assertTrue(QubitState.fromBloch(Math.PI / 2, 0).sameRayAs(QubitState.plus(), TOL));
        assertTrue(QubitState.fromBloch(Math.PI / 2, Math.PI).sameRayAs(QubitState.minus(), TOL));
        assertTrue(
                QubitState.fromBloch(Math.PI / 2, Math.PI / 2).sameRayAs(QubitState.plusI(), TOL));
        assertTrue(
                QubitState.fromBloch(Math.PI / 2, -Math.PI / 2)
                        .sameRayAs(QubitState.minusI(), TOL));
    }

    @Test
    @DisplayName("fromBloch: P(|1>) = sin^2(theta/2), always normalised")
    void blochProbability() {
        for (double theta = 0; theta <= Math.PI + 1e-12; theta += Math.PI / 16) {
            double t = Math.min(theta, Math.PI);
            for (double phi = -3; phi <= 3; phi += 0.75) {
                QubitState s = QubitState.fromBloch(t, phi);
                double expected = Math.sin(t / 2) * Math.sin(t / 2);
                assertEquals(expected, s.probability1(), TOL);
                assertEquals(1.0, s.normSquared(), TOL);
            }
        }
    }

    @Test
    @DisplayName("fromBloch validates its angles")
    void blochValidation() {
        assertThrows(IllegalArgumentException.class, () -> QubitState.fromBloch(-0.1, 0));
        assertThrows(IllegalArgumentException.class, () -> QubitState.fromBloch(3.2, 0));
        assertThrows(IllegalArgumentException.class, () -> QubitState.fromBloch(1, Double.NaN));
    }

    @Test
    @DisplayName("fromBloch's phi is the relative phase")
    void blochPhiIsRelativePhase() {
        for (double phi = -3.0; phi <= 3.0; phi += 0.5) {
            QubitState s = QubitState.fromBloch(1.0, phi);
            assertEquals(phi, s.relativePhase().getAsDouble(), TOL);
        }
    }

    @Test
    @DisplayName("fromDecay reproduces the classical decay law")
    void fromDecayMatchesLaw() {
        DecayModel m = new DecayModel(0.5);
        QubitState s = QubitState.fromDecay(m, 2.0, 0.0);
        assertEquals(Math.exp(-1.0), s.probability0(), TOL);
        assertEquals(1.0 - Math.exp(-1.0), s.probability1(), TOL);
        assertEquals(1.0, s.normSquared(), TOL);
    }

    @Test
    @DisplayName("fromDecay at t = 0 is the undecayed state, whatever the phase")
    void fromDecayAtZero() {
        QubitState s = QubitState.fromDecay(new DecayModel(3.0), 0.0, 1.234);
        assertTrue(s.sameRayAs(QubitState.basis0(), TOL));
    }

    @Test
    @DisplayName("fromDecay at the half-life is an equal-probability superposition")
    void fromDecayAtHalfLife() {
        DecayModel m = DecayModel.fromHalfLife(4.0);
        QubitState s = QubitState.fromDecay(m, 4.0, 0.0);
        assertEquals(0.5, s.probability0(), TOL);
        assertEquals(0.5, s.probability1(), TOL);
    }

    @Test
    @DisplayName("fromDecay carries the requested relative phase")
    void fromDecayPhase() {
        QubitState s = QubitState.fromDecay(DecayModel.fromHalfLife(1.0), 1.0, 0.8);
        assertEquals(0.8, s.relativePhase().getAsDouble(), TOL);
    }
}
