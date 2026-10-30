package dev.quantumlab.simulation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The scope contract for the composite-system (Mode D) screen. */
class CompositeExplanationTest {

    private static boolean anyContains(List<String> lines, String fragment) {
        return lines.stream().anyMatch(l -> l.contains(fragment));
    }

    @Test
    @DisplayName("the composite entry says the detector is a stand-in, it is not the cat, nothing evolves, nothing collapses, and CHSH says nothing about a cat")
    void caveats() {
        ModeExplanation m = ExplanationCatalog.composite();
        assertFalse(m.represents().isEmpty());
        List<String> no = m.doesNotRepresent();
        assertTrue(anyContains(no, "two-level stand-in"));
        assertTrue(anyContains(no, "not a model of a real detector"));
        assertTrue(anyContains(no, "Not the cat"));
        assertTrue(anyContains(no, "No dynamics and no decoherence"));
        assertTrue(anyContains(no, "exact, not sampled"));
        assertTrue(anyContains(no, "does not solve the measurement problem"));
        assertTrue(anyContains(no, "consciousness"));
        assertTrue(anyContains(no, "no spatial separation or signalling"));
    }

    @Test
    @DisplayName("the composite entry states the joint Born rule, the mixture, the entanglement measures and both CHSH bounds")
    void mathematics() {
        ModeExplanation m = ExplanationCatalog.composite();
        assertTrue(m.mathematics().contains("P(i, j) = |⟨bᵢ ⊗ cⱼ | ψ⟩|²"));
        assertTrue(m.mathematics().contains("ρ = p0 |00⟩⟨00| + (1 − p0) |11⟩⟨11|"));
        assertTrue(m.mathematics().contains("Concurrence"));
        assertTrue(m.mathematics().contains("2√2"));
        assertTrue(m.mathematics().contains("2√(1 + C²)"));
        assertTrue(anyContains(m.represents(), "looks the same for the entangled state and the mixture"));
    }

    @Test
    @DisplayName("the overall scope lists the composite model, keeps it separate from the cat, and still says decoherence is not implemented")
    void generalScope() {
        ModeExplanation g = ExplanationCatalog.general();
        assertTrue(anyContains(g.represents(), "Two qubits, a nucleus and an idealised detector"));
        assertTrue(anyContains(g.doesNotRepresent(), "composite-system screen is a separate two-qubit model"));
        assertTrue(anyContains(g.doesNotRepresent(), "Decoherence (Mode E) is planned but not implemented"));
        assertFalse(anyContains(g.doesNotRepresent(), "Entanglement of the nucleus with the apparatus (Mode D)"));
    }
}
