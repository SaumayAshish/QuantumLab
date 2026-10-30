package dev.quantumlab.simulation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The scope contract for the tunnelling screen. */
class TunnellingExplanationTest {

    private static boolean anyContains(List<String> lines, String fragment) {
        return lines.stream().anyMatch(l -> l.contains(fragment));
    }

    @Test
    @DisplayName("the tunnelling entry says nothing moves, nothing is measured, and it is idealised")
    void caveats() {
        ModeExplanation m = ExplanationCatalog.tunnelling();
        assertFalse(m.represents().isEmpty());
        List<String> no = m.doesNotRepresent();
        assertTrue(anyContains(no, "Nothing moves"));
        assertTrue(anyContains(no, "not a wave packet"));
        assertTrue(anyContains(no, "no time of passage"));
        assertTrue(anyContains(no, "No measurement or collapse"));
        assertTrue(anyContains(no, "T is a probability"));
        assertTrue(anyContains(no, "idealised"));
    }

    @Test
    @DisplayName("the tunnelling entry states the formulas it uses and contrasts it with classical motion")
    void mathematicsAndContrast() {
        ModeExplanation m = ExplanationCatalog.tunnelling();
        assertTrue(m.mathematics().contains("sinh²(κa)"));
        assertTrue(m.mathematics().contains("sin²(ka)"));
        assertTrue(m.mathematics().contains("e^(−2κa)"));
        assertTrue(anyContains(m.represents(), "classical particle is always turned back"));
        assertTrue(anyContains(m.represents(), "classical particle always gets over"));
    }

    @Test
    @DisplayName("the overall scope mentions the transmission model and keeps the cat disclaimers")
    void generalScope() {
        ModeExplanation g = ExplanationCatalog.general();
        assertTrue(anyContains(g.represents(), "transmission of a particle through rectangular barriers"));
        assertTrue(anyContains(g.doesNotRepresent(), "tunnelling screen do not describe the cat"));
        assertTrue(anyContains(g.doesNotRepresent(), "does not simulate a macroscopic cat"));
    }
}
