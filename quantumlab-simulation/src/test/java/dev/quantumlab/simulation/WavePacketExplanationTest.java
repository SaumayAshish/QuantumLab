package dev.quantumlab.simulation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The scope contract for the wave-packet screen.
 */
class WavePacketExplanationTest {

    private static boolean anyContains(List<String> lines, String fragment) {
        return lines.stream().anyMatch(l -> l.contains(fragment));
    }

    @Test
    @DisplayName("the wave-packet entry says it shows a density, shows no collapse, follows no particle, and is idealised")
    void caveats() {
        ModeExplanation m = ExplanationCatalog.wavePacket();
        assertFalse(m.represents().isEmpty());
        List<String> no = m.doesNotRepresent();
        assertTrue(anyContains(no, "hard walls"));
        assertTrue(anyContains(no, "rounded to a whole number of cells"));
        assertTrue(anyContains(no, "No measurement or collapse"));
        assertTrue(anyContains(no, "not a classical particle splitting in two"));
        assertTrue(anyContains(no, "no passage time"));
        assertTrue(anyContains(no, "idealised"));
        assertTrue(anyContains(no, "not the cat"));
    }

    @Test
    @DisplayName("the wave-packet entry states the scheme and the prediction it is compared with")
    void mathematics() {
        ModeExplanation m = ExplanationCatalog.wavePacket();
        assertTrue(m.mathematics().contains("Crank–Nicolson"));
        assertTrue(m.mathematics().contains("exp(−2σ²(k − k0)²)"));
        assertTrue(anyContains(m.represents(), "not T at the central energy alone"));
    }

    @Test
    @DisplayName("the overall scope lists the wave-packet model and keeps it separate from the cat")
    void generalScope() {
        ModeExplanation g = ExplanationCatalog.general();
        assertTrue(anyContains(g.represents(), "Gaussian wave packet"));
        assertTrue(anyContains(g.doesNotRepresent(), "wave-packet screen is likewise a separate model"));
        assertTrue(anyContains(g.doesNotRepresent(), "tunnelling screen do not describe the cat"));
    }
}
