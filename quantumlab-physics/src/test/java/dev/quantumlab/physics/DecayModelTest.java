package dev.quantumlab.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.math.Tolerances;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DecayModelTest {

    private static final double TOL = Tolerances.STRICT;

    @Test
    @DisplayName("survival and decay probabilities sum to one")
    void probabilitiesSumToOne() {
        DecayModel m = new DecayModel(0.37);
        for (double t = 0; t <= 20; t += 0.7) {
            assertEquals(1.0, m.survivalProbability(t) + m.decayProbability(t), TOL);
        }
    }

    @Test
    @DisplayName("nothing has decayed at t = 0")
    void noDecayAtZero() {
        assertEquals(0.0, new DecayModel(5.0).decayProbability(0.0), 0.0);
        assertEquals(1.0, new DecayModel(5.0).survivalProbability(0.0), 0.0);
    }

    @Test
    @DisplayName("a stable nucleus (lambda = 0) never decays and has infinite half-life")
    void stableNucleus() {
        DecayModel m = new DecayModel(0.0);
        assertEquals(0.0, m.decayProbability(1e9), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, m.halfLife(), 0.0);
    }

    @Test
    @DisplayName("exactly half has decayed after one half-life")
    void halfLife() {
        DecayModel m = new DecayModel(0.25);
        assertEquals(0.5, m.decayProbability(m.halfLife()), TOL);
        assertEquals(Math.log(2.0) / 0.25, m.halfLife(), TOL);
    }

    @Test
    @DisplayName("fromHalfLife round-trips")
    void fromHalfLifeRoundTrip() {
        assertEquals(3.5, DecayModel.fromHalfLife(3.5).halfLife(), TOL);
        assertEquals(0.75, DecayModel.fromHalfLife(2.0).decayProbability(4.0), TOL);
    }

    @Test
    @DisplayName("decay probability is strictly increasing in time")
    void monotonic() {
        DecayModel m = new DecayModel(0.9);
        double previous = -1.0;
        for (double t = 0; t <= 10; t += 0.5) {
            double d = m.decayProbability(t);
            assertTrue(d > previous);
            previous = d;
        }
    }

    @Test
    @DisplayName("tiny lambda*t keeps full relative precision (no catastrophic cancellation)")
    void smallArgumentPrecision() {
        double x = 1e-12;
        double expected = x - x * x / 2; // series of 1 - e^{-x}
        double got = new DecayModel(x).decayProbability(1.0);
        assertTrue(Math.abs(got - expected) / expected < 1e-9, "relative error too large: " + got);
    }

    @Test
    @DisplayName("invalid parameters are rejected")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> new DecayModel(-0.1));
        assertThrows(IllegalArgumentException.class, () -> new DecayModel(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new DecayModel(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> DecayModel.fromHalfLife(0.0));
        assertThrows(IllegalArgumentException.class, () -> new DecayModel(1.0).decayProbability(-1.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DecayModel(1.0).survivalProbability(Double.NaN));
    }
}
