package dev.quantumlab.simulation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The scope contract for the one-dimensional solver, and the updated overall scope. */
class SchrodingerExplanationTest {

    private static boolean anyContains(List<String> lines, String fragment) {
        return lines.stream().anyMatch(l -> l.contains(fragment));
    }

    @Test
    @DisplayName("the solver's explanation states what it represents and what it does not")
    void qualified() {
        ModeExplanation m = ExplanationCatalog.schrodinger1d();
        assertFalse(m.title().isBlank());
        assertFalse(m.summary().isBlank());
        assertFalse(m.mathematics().isBlank());
        assertFalse(m.represents().isEmpty());
        assertFalse(m.doesNotRepresent().isEmpty());
    }

    @Test
    @DisplayName("the solver says nothing evolves in time, the walls are hard, and it is not the cat")
    void solverCaveats() {
        List<String> no = ExplanationCatalog.schrodinger1d().doesNotRepresent();
        assertTrue(anyContains(no, "Nothing evolves in time"));
        assertTrue(anyContains(no, "no measurement or collapse is shown"));
        assertTrue(anyContains(no, "hard"));
        assertTrue(anyContains(no, "artificial box"));
        assertTrue(anyContains(no, "Only the lowest few levels are accurate"));
        assertTrue(anyContains(no, "ħ = m = 1"));
        assertTrue(anyContains(no, "not the nucleus or the cat"));
    }

    @Test
    @DisplayName("the solver states its equation, its discretisation and both exact references")
    void solverMathematics() {
        String m = ExplanationCatalog.schrodinger1d().mathematics();
        assertTrue(m.contains("−½ ψ″(x) + V(x) ψ(x) = E ψ(x)"));
        assertTrue(m.contains("(ψ_{i−1} − 2ψ_i + ψ_{i+1}) / h²"));
        assertTrue(m.contains("n²π²/(2L²)"));
        assertTrue(m.contains("ω(n + ½)"));
    }

    @Test
    @DisplayName("the overall scope now mentions the solver and keeps every earlier disclaimer")
    void generalScope() {
        ModeExplanation g = ExplanationCatalog.general();
        assertTrue(g.summary().contains("single particle in one dimension"));
        assertTrue(anyContains(g.represents(), "one particle in one dimension"));
        List<String> no = g.doesNotRepresent();
        assertTrue(anyContains(no, "does not simulate a macroscopic cat"));
        assertTrue(anyContains(no, "measurement problem"));
        assertTrue(anyContains(no, "consciousness"));
        assertTrue(anyContains(no, "planned but not implemented"));
        assertTrue(anyContains(no, "solver does not describe the cat"));
    }
}
