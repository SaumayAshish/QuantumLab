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
                List.of(ExplanationText.forClassicalScreen(), ExplanationText.forMeasurementScreen())) {
            for (ModeExplanation e : screen) {
                assertTrue(!e.doesNotRepresent().isEmpty(), "no limitation stated in " + e.title());
            }
        }
    }
}
