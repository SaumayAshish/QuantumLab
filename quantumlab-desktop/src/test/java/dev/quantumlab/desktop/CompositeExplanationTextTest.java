package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.simulation.ExplanationCatalog;
import dev.quantumlab.simulation.ModeExplanation;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompositeExplanationTextTest {

    @Test
    @DisplayName("the composite screen explains the composite model first and the overall scope last, verbatim")
    void orderAndText() {
        List<ModeExplanation> list = ExplanationText.forCompositeScreen();
        assertEquals(
            List.of(ExplanationCatalog.composite().title(), ExplanationCatalog.general().title()),
            list.stream().map(ModeExplanation::title).toList());
        String text = ExplanationText.format(list.get(0));
        for (String line : ExplanationCatalog.composite().doesNotRepresent()) {
            assertTrue(text.contains("• " + line));
        }
    }
}
