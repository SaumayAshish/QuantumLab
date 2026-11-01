package dev.quantumlab.simulation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The scope caveats are part of the product's scientific contract. These tests fail if one is
 * deleted or watered down, so a future edit cannot quietly turn an honest model into a misleading
 * one.
 */
class ExplanationCatalogTest {

    private static List<ModeExplanation> all() {
        return List.of(
            ExplanationCatalog.classical(),
            ExplanationCatalog.superposition(),
            ExplanationCatalog.measurement(),
            ExplanationCatalog.schrodinger1d(),
            ExplanationCatalog.tunnelling(),
            ExplanationCatalog.wavePacket(),
            ExplanationCatalog.composite(),
            ExplanationCatalog.decoherence(),
            ExplanationCatalog.interference(),
            ExplanationCatalog.general());
    }

    private static boolean anyContains(List<String> lines, String fragment) {
        return lines.stream().anyMatch(l -> l.contains(fragment));
    }

    @Test
    @DisplayName("every mode states both what it represents and what it does not")
    void everyModeIsQualified() {
        for (ModeExplanation m : all()) {
            assertFalse(m.title().isBlank());
            assertFalse(m.summary().isBlank());
            assertFalse(m.mathematics().isBlank());
            assertFalse(m.represents().isEmpty(), m.title() + " lists nothing it represents");
            assertFalse(m.doesNotRepresent().isEmpty(), m.title() + " lists no limitations");
        }
    }

    @Test
    @DisplayName("Mode A says it is not a quantum-state simulation and is not a superposition")
    void classicalCaveats() {
        List<String> no = ExplanationCatalog.classical().doesNotRepresent();
        assertTrue(anyContains(no, "not a quantum-state simulation"));
        assertTrue(anyContains(no, "ignorance"));
    }

    @Test
    @DisplayName("Mode B flags the decay-prepared state as a modelling convenience")
    void superpositionCaveats() {
        List<String> no = ExplanationCatalog.superposition().doesNotRepresent();
        assertTrue(anyContains(no, "modelling convenience"));
        assertTrue(anyContains(no, "consciousness"));
        assertTrue(
            anyContains(ExplanationCatalog.superposition().represents(), "classical mixture"));
    }

    @Test
    @DisplayName("Mode C says a seeded sequence is deterministic and not a prediction of quantum outcomes")
    void measurementCaveats() {
        List<String> no = ExplanationCatalog.measurement().doesNotRepresent();
        assertTrue(anyContains(no, "deterministic"));
        assertTrue(anyContains(no, "does not mean individual quantum outcomes are predictable"));
        assertTrue(anyContains(no, "no mechanism for collapse"));
    }

    @Test
    @DisplayName("the general scope disclaims the cat, the measurement problem, consciousness, and and keeps the decoherence screen separate")
    void generalCaveats() {
        List<String> no = ExplanationCatalog.general().doesNotRepresent();
        assertTrue(anyContains(no, "does not simulate a macroscopic cat"));
        assertTrue(anyContains(no, "measurement problem"));
        assertTrue(anyContains(no, "consciousness"));
        assertTrue(anyContains(no, "decoherence screen is a separate single-qubit model"));
    }
    @Test
    @DisplayName("the interference mode says the phase is a free input and nothing is measured")
    void interferenceCaveats() {
        List<String> no = ExplanationCatalog.interference().doesNotRepresent();
        assertTrue(anyContains(no, "free input"));
        assertTrue(anyContains(no, "Nothing is sampled, measured or collapsed"));
        assertTrue(anyContains(no, "Not the cat"));
        assertTrue(anyContains(no, "not a simulation of an apparatus"));
        assertTrue(anyContains(ExplanationCatalog.interference().represents(), "classical mixture"));
    }

    @Test
    @DisplayName("the general scope keeps the interference screen separate from the cat")
    void generalKeepsInterferenceSeparate() {
        List<String> no = ExplanationCatalog.general().doesNotRepresent();
        assertTrue(anyContains(no, "interference screen is a separate single-qubit model"));
    }

    @Test
    @DisplayName("the explanation lists are immutable")
    void immutable() {
        org.junit.jupiter.api.Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> ExplanationCatalog.general().doesNotRepresent().clear());
    }
}
