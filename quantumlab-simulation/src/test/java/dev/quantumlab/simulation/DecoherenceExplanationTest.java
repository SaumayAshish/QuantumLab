package dev.quantumlab.simulation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The scope contract for the decoherence (Mode E) screen. */
class DecoherenceExplanationTest {

    private static boolean anyContains(List<String> lines, String fragment) {
        return lines.stream().anyMatch(l -> l.contains(fragment));
    }

    @Test
    @DisplayName("the decoherence entry says the rate is an input, it is not the cat, it does not explain a single outcome, it is only dephasing, and it is exact")
    void caveats() {
        ModeExplanation m = ExplanationCatalog.decoherence();
        assertFalse(m.represents().isEmpty());
        List<String> no = m.doesNotRepresent();
        assertTrue(anyContains(no, "an input you choose, not derived from any physical environment"));
        assertTrue(anyContains(no, "no environment is simulated"));
        assertTrue(anyContains(no, "Not the cat"));
        assertTrue(anyContains(no, "does not by itself explain why one outcome is observed"));
        assertTrue(anyContains(no, "does not solve the measurement problem"));
        assertTrue(anyContains(no, "Only pure dephasing is modelled"));
        assertTrue(anyContains(no, "exact, not sampled"));
        assertTrue(anyContains(no, "consciousness"));
    }

    @Test
    @DisplayName("the decoherence entry states the dephasing law, the mixed-state Born rule, the fringe and the purity")
    void mathematics() {
        ModeExplanation m = ExplanationCatalog.decoherence();
        assertTrue(m.mathematics().contains("ρ₀₁(t) = ρ₀₁(0) e^{−γt}"));
        assertTrue(m.mathematics().contains("P(k) = ⟨bₖ|ρ|bₖ⟩"));
        assertTrue(m.mathematics().contains("P(φ) = ½ + Re(ρ₀₁ e^{iφ})"));
        assertTrue(m.mathematics().contains("Tr ρ² = ρ₀₀² + ρ₁₁² + 2|ρ₀₁|²"));
        assertTrue(anyContains(m.represents(), "losing coherence and a single outcome appearing"));
    }

    @Test
    @DisplayName("the overall scope lists the decoherence model, keeps it separate from the cat, and no longer lists an unbuilt mode")
    void generalScope() {
        ModeExplanation g = ExplanationCatalog.general();
        assertTrue(anyContains(g.represents(), "One qubit losing its coherence at an assumed dephasing rate"));
        assertTrue(anyContains(g.doesNotRepresent(), "decoherence screen is a separate single-qubit model"));
        assertTrue(anyContains(g.doesNotRepresent(), "does not describe the cat, a real environment"));
        assertFalse(anyContains(g.doesNotRepresent(), "Mode E"));
        assertFalse(anyContains(ExplanationCatalog.composite().doesNotRepresent(), "planned as Mode E"));
    }
}
