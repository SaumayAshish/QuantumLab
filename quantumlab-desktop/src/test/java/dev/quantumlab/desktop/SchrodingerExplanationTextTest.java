package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.simulation.ExplanationCatalog;
import dev.quantumlab.simulation.ModeExplanation;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SchrodingerExplanationTextTest {

    @Test
    @DisplayName("the Schrödinger screen explains the solver first and the overall scope last")
    void order() {
        List<ModeExplanation> list = ExplanationText.forSchrodingerScreen();
        assertEquals(
                List.of(ExplanationCatalog.schrodinger1d().title(), ExplanationCatalog.general().title()),
                list.stream().map(ModeExplanation::title).toList());
    }

    @Test
    @DisplayName("every capability and limitation of the solver is shown verbatim, limitations after capabilities")
    void shownVerbatim() {
        ModeExplanation e = ExplanationCatalog.schrodinger1d();
        String text = ExplanationText.format(e);
        for (String line : e.represents()) {
            assertTrue(text.contains("• " + line));
        }
        for (String line : e.doesNotRepresent()) {
            assertTrue(text.contains("• " + line));
        }
        int models = text.indexOf(ExplanationText.MODELS_HEADING + "\n");
        int doesNot = text.indexOf(ExplanationText.DOES_NOT_MODEL_HEADING + "\n");
        assertTrue(models >= 0 && doesNot > models);
    }
}
