package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.simulation.ExplanationCatalog;
import dev.quantumlab.simulation.ModeExplanation;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TunnellingExplanationTextTest {

    @Test
    @DisplayName("the tunnelling screen explains tunnelling first and the overall scope last, verbatim")
    void orderAndText() {
        List<ModeExplanation> list = ExplanationText.forTunnellingScreen();
        assertEquals(
            List.of(ExplanationCatalog.tunnelling().title(), ExplanationCatalog.general().title()),
            list.stream().map(ModeExplanation::title).toList());
        String text = ExplanationText.format(list.get(0));
        for (String line : ExplanationCatalog.tunnelling().doesNotRepresent()) {
            assertTrue(text.contains("• " + line));
        }
    }
}
