package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.simulation.ExplanationCatalog;
import dev.quantumlab.simulation.ModeExplanation;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExplanationScreensTest {

    @Test
    @DisplayName("the classical screen explains the classical mode first and the overall scope last")
    void classicalScreen() {
        List<ModeExplanation> list = ExplanationText.forClassicalScreen();
        assertEquals(
            List.of(ExplanationCatalog.classical().title(), ExplanationCatalog.general().title()),
            list.stream().map(ModeExplanation::title).toList());
    }

    @Test
    @DisplayName("the interference screen explains interference first and the overall scope last")
    void interferenceScreen() {
        List<ModeExplanation> list = ExplanationText.forInterferenceScreen();
        assertEquals(
            List.of(
                ExplanationCatalog.interference().title(),
                ExplanationCatalog.general().title()),
            list.stream().map(ModeExplanation::title).toList());
    }

    @Test
    @DisplayName("the interference footer is built from the catalogue's first limitations")
    void interferenceScopeNote() {
        String note = ScopeNote.forInterferenceScreen();
        assertEquals(
            "Scope: " + ExplanationCatalog.interference().doesNotRepresent().get(0)
                + " " + ExplanationCatalog.general().doesNotRepresent().get(0),
            note);
    }

    @Test
    @DisplayName("the measurement screen still ends with the overall scope")
    void measurementScreenEndsWithScope() {
        List<ModeExplanation> list = ExplanationText.forMeasurementScreen();
        assertEquals(3, list.size());
        assertEquals(ExplanationCatalog.general().title(), list.get(list.size() - 1).title());
    }

    @Test
    @DisplayName("every screen's explanations state at least one limitation")
    void everyScreenStatesLimitations() {
        for (List<ModeExplanation> screen :
            List.of(
                ExplanationText.forClassicalScreen(),
                ExplanationText.forMeasurementScreen(),
                ExplanationText.forInterferenceScreen())) {
            for (ModeExplanation e : screen) {
                assertTrue(!e.doesNotRepresent().isEmpty(), "no limitation stated in " + e.title());
            }
        }
    }
}
