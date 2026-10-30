package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.simulation.ExplanationCatalog;
import dev.quantumlab.simulation.ModeExplanation;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WavePacketExplanationTextTest {

    @Test
    @DisplayName("the wave-packet screen explains the wave packet first and the overall scope last, verbatim")
    void orderAndText() {
        List<ModeExplanation> list = ExplanationText.forWavePacketScreen();
        assertEquals(
            List.of(ExplanationCatalog.wavePacket().title(), ExplanationCatalog.general().title()),
            list.stream().map(ModeExplanation::title).toList());
        String text = ExplanationText.format(list.get(0));
        for (String line : ExplanationCatalog.wavePacket().doesNotRepresent()) {
            assertTrue(text.contains("• " + line));
        }
    }
}
