package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.simulation.ExplanationCatalog;
import dev.quantumlab.simulation.ModeExplanation;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExplanationTextTest {

    private static List<ModeExplanation> everyCatalogEntry() {
        return List.of(
                ExplanationCatalog.classical(),
                ExplanationCatalog.superposition(),
                ExplanationCatalog.measurement(),
                ExplanationCatalog.general());
    }

    @Test
    @DisplayName("the text contains the title, summary and mathematics")
    void containsCoreSections() {
        ModeExplanation e = ExplanationCatalog.measurement();
        String text = ExplanationText.format(e);
        assertTrue(text.startsWith(e.title()));
        assertTrue(text.contains(e.summary()));
        assertTrue(text.contains("Mathematics\n" + e.mathematics()));
    }

    @Test
    @DisplayName("every capability and every limitation in the catalog is shown verbatim")
    void nothingIsDropped() {
        for (ModeExplanation e : everyCatalogEntry()) {
            String text = ExplanationText.format(e);
            for (String line : e.represents()) {
                assertTrue(text.contains("• " + line), "missing capability in " + e.title());
            }
            for (String line : e.doesNotRepresent()) {
                assertTrue(text.contains("• " + line), "missing limitation in " + e.title());
            }
        }
    }

    @Test
    @DisplayName("limitations get their own heading, after the capabilities, in every mode")
    void limitationsAreProminent() {
        for (ModeExplanation e : everyCatalogEntry()) {
            String text = ExplanationText.format(e);
            int models = text.indexOf(ExplanationText.MODELS_HEADING + "\n");
            int doesNot = text.indexOf(ExplanationText.DOES_NOT_MODEL_HEADING + "\n");
            assertTrue(models >= 0 && doesNot > models, "headings out of order in " + e.title());
        }
    }

    @Test
    @DisplayName("an empty list is shown as 'none stated' rather than silently omitted")
    void emptyLists() {
        ModeExplanation bare = new ModeExplanation("T", "S", "M", List.of(), List.of());
        String text = ExplanationText.format(bare);
        assertEquals(2, text.split("• \\(none stated\\)", -1).length - 1);
    }

    @Test
    @DisplayName("the measurement screen shows its own mode, the superposition it builds on, and the global scope")
    void screenContents() {
        List<ModeExplanation> shown = ExplanationText.forMeasurementScreen();
        assertEquals(
                List.of(
                        ExplanationCatalog.measurement().title(),
                        ExplanationCatalog.superposition().title(),
                        ExplanationCatalog.general().title()),
                shown.stream().map(ModeExplanation::title).toList());
    }
}
