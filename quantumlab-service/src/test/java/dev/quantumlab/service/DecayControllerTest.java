package dev.quantumlab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.simulation.ExplanationCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DecayControllerTest {

    private final DecayController controller = new DecayController();

    @Test
    void matchesTheClosedFormForRateOneAndTimeTwo() {
        DecayController.DecayResult r = controller.decay("1", "2");

        // 1 - e^-2 and e^-2, written out independently of the code under test.
        assertEquals(0.8646647167633873, r.decayProbability(), 1e-12);
        assertEquals(0.1353352832366127, r.survivalProbability(), 1e-12);
        assertEquals(Math.log(2.0), r.halfLife(), 1e-12);
        assertEquals(1.0, r.decayRate(), 0.0);
        assertEquals(2.0, r.time(), 0.0);
    }

    @Test
    void aStableNucleusHasNoHalfLife() {
        DecayController.DecayResult r = controller.decay("0", "5");

        assertEquals(0.0, r.decayProbability(), 1e-15);
        assertEquals(1.0, r.survivalProbability(), 1e-15);
        assertNull(r.halfLife());
    }

    @Test
    void carriesTheClassicalExplanationFromTheCatalogue() {
        assertEquals(ExplanationCatalog.classical(), controller.decay("1", "2").explanation());
    }

    @Test
    void rejectsANegativeRate() {
        assertThrows(BadRequestException.class, () -> controller.decay("-0.1", "2"));
    }

    @Test
    void rejectsANegativeTime() {
        assertThrows(BadRequestException.class, () -> controller.decay("1", "-2"));
    }

    @Test
    void reportsAMissingParameterByName() {
        BadRequestException noRate =
            assertThrows(BadRequestException.class, () -> controller.decay(null, "2"));
        BadRequestException noTime =
            assertThrows(BadRequestException.class, () -> controller.decay("1", null));

        assertTrue(noRate.getMessage().contains("rate"));
        assertTrue(noTime.getMessage().contains("time"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "0x10", "1d", "1e999", "abc", "", "1,5"})
    void rejectsTextThatIsNotAPlainDecimal(String text) {
        BadRequestException e =
            assertThrows(BadRequestException.class, () -> controller.decay(text, "2"));

        assertTrue(e.getMessage().contains("rate"));
    }

    @Test
    void theErrorHandlerReturnsTheMessage() {
        ErrorHandler.ErrorBody body = new ErrorHandler().badRequest(new BadRequestException("nope"));

        assertEquals("nope", body.error());
    }
}
