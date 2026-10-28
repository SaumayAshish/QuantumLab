package dev.quantumlab.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.physics.QubitState;
import dev.quantumlab.physics.StandardBasis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConfigTest {

    @Test
    @DisplayName("classical config validates rate and time")
    void classicalValidation() {
        assertThrows(IllegalArgumentException.class, () -> new ClassicalDecayConfig(-1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ClassicalDecayConfig(1, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ClassicalDecayConfig(Double.NaN, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ClassicalDecayConfig(1, Double.NaN, 0));
        new ClassicalDecayConfig(0, 0, 0); // boundary values are valid
    }

    @Test
    @DisplayName("classical config exposes the decay probability")
    void classicalProbability() {
        ClassicalDecayConfig c = new ClassicalDecayConfig(Math.log(2), 1.0, 5);
        assertEquals(0.5, c.decayProbability(), 1e-12);
        assertEquals(1.0, c.model().halfLife(), 1e-12);
    }

    @Test
    @DisplayName("quantum config rejects un-normalised and non-finite amplitudes")
    void quantumValidation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuantumMeasurementConfig(1, 0, 1, 0, StandardBasis.COMPUTATIONAL, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuantumMeasurementConfig(Double.NaN, 0, 0, 0, StandardBasis.COMPUTATIONAL, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuantumMeasurementConfig(1, 0, 0, 0, null, 0));
    }

    @Test
    @DisplayName("quantum config round-trips a prepared state")
    void quantumRoundTrip() {
        QubitState s = QubitState.fromBloch(1.1, -0.7);
        QuantumMeasurementConfig c = QuantumMeasurementConfig.of(s, StandardBasis.HADAMARD, 42);
        assertTrue(c.state().sameRayAs(s, 1e-12));
        assertEquals(s.alpha().re(), c.alphaRe(), 0.0);
        assertEquals(s.beta().im(), c.betaIm(), 0.0);
        assertEquals(StandardBasis.HADAMARD, c.basis());
        assertEquals(42L, c.seed());
    }

    @Test
    @DisplayName("configs compare by value")
    void valueEquality() {
        QubitState s = QubitState.plus();
        assertEquals(
                QuantumMeasurementConfig.of(s, StandardBasis.COMPUTATIONAL, 1),
                QuantumMeasurementConfig.of(s, StandardBasis.COMPUTATIONAL, 1));
        assertEquals(new ClassicalDecayConfig(1, 2, 3), new ClassicalDecayConfig(1, 2, 3));
    }

    @Test
    @DisplayName("mode ids are stable (they appear in exported files)")
    void modeIds() {
        assertEquals("classical-decay", new ClassicalDecayConfig(1, 1, 0).modeId());
        assertEquals(
                "quantum-measurement",
                QuantumMeasurementConfig.of(QubitState.plus(), StandardBasis.COMPUTATIONAL, 0)
                        .modeId());
    }

    @Test
    @DisplayName("a switch over ExperimentConfig is exhaustive without a default (sealed hierarchy)")
    void sealedExhaustive() {
        ExperimentConfig c = new ClassicalDecayConfig(1, 1, 0);
        // This compiles only while ExperimentConfig permits exactly these two record types.
        String kind =
                switch (c) {
                    case ClassicalDecayConfig a -> "classical";
                    case QuantumMeasurementConfig q -> "quantum";
                };
        assertEquals("classical", kind);
    }
}
