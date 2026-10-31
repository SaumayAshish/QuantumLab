package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.simulation.ExplanationCatalog;
import dev.quantumlab.simulation.ModeExplanation;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScopeNoteTest {

    private static final List<List<ModeExplanation>> SCREENS =
        List.of(
            ExplanationText.forSchrodingerScreen(),
            ExplanationText.forTunnellingScreen(),
            ExplanationText.forWavePacketScreen(),
            ExplanationText.forCompositeScreen(),
            ExplanationText.forDecoherenceScreen(),
            ExplanationText.forClassicalScreen(),
            ExplanationText.forMeasurementScreen());

    private static String generalLimit() {
        return ExplanationCatalog.general().doesNotRepresent().get(0);
    }

    @Test
    void everyScreenCarriesTheCatCaveatAndTheMeasurementProblemCaveat() {
        for (List<ModeExplanation> screen : SCREENS) {
            String note = ScopeNote.of(screen);
            assertTrue(note.contains("macroscopic cat"), note);
            assertTrue(note.contains("measurement problem"), note);
        }
    }

    @Test
    void everyScreenShowsItsOwnFirstLimitationVerbatim() {
        for (List<ModeExplanation> screen : SCREENS) {
            String own = screen.get(0).doesNotRepresent().get(0);
            assertTrue(ScopeNote.of(screen).contains(own), own);
        }
    }

    @Test
    void ownLimitationComesBeforeTheGeneralOne() {
        for (List<ModeExplanation> screen : SCREENS) {
            String note = ScopeNote.of(screen);
            String own = screen.get(0).doesNotRepresent().get(0);
            assertTrue(note.indexOf(own) < note.indexOf(generalLimit()), note);
        }
    }

    @Test
    void aSingleExplanationIsNotRepeated() {
        assertEquals(
            ScopeNote.PREFIX + generalLimit(),
            ScopeNote.of(List.of(ExplanationCatalog.general())));
    }

    @Test
    void noExplanationsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ScopeNote.of(List.of()));
    }
}
