package dev.quantumlab.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.simulation.ExplanationCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LevelsControllerTest {

    private final LevelsController controller = new LevelsController();

    private LevelsController.LevelsResult well(String points, String levels) {
        return controller.levels("well", "0", "1", points, levels, null);
    }

    private String message(Runnable call) {
        return assertThrows(BadRequestException.class, call::run).getMessage();
    }

    @Test
    void infiniteWellMatchesAnIndependentDenseDiagonalisation() {
        LevelsController.LevelsResult r = well("100", "3");

        double[] numerical = {4.934404339433664, 19.732843640209502, 44.3810013680459};
        double[] reference = {4.934802200544679, 19.739208802178716, 44.41321980490211};
        assertEquals(3, r.levels().size());
        for (int k = 0; k < 3; k++) {
            assertEquals(k + 1, r.levels().get(k).n());
            assertEquals(numerical[k], r.levels().get(k).numerical(), 1e-8);
            assertEquals(reference[k], r.levels().get(k).reference(), 1e-12);
        }
        assertEquals(1.0 / 101.0, r.spacing(), 1e-15);
        assertEquals(100, r.gridPoints());
    }

    @Test
    void harmonicOscillatorMatchesAnIndependentDenseDiagonalisation() {
        LevelsController.LevelsResult r =
            controller.levels("harmonic", "-8", "8", "800", "3", "1");

        double[] numerical = {0.4999875308806908, 1.499937653159729, 2.4998378946059003};
        assertEquals(1.0, r.omega(), 0.0);
        for (int k = 0; k < 3; k++) {
            assertEquals(k, r.levels().get(k).n());
            assertEquals(numerical[k], r.levels().get(k).numerical(), 1e-8);
            assertEquals(k + 0.5, r.levels().get(k).reference(), 1e-15);
        }
    }

    @Test
    void errorsAreNumericalMinusReference() {
        for (LevelsController.Level l : well("100", "5").levels()) {
            assertEquals(l.numerical() - l.reference(), l.error(), 1e-15);
            assertEquals(l.error() / l.reference(), l.relativeError(), 1e-15);
        }
    }

    @Test
    void carriesTheSchrodingerExplanationFromTheCatalogue() {
        assertEquals(ExplanationCatalog.schrodinger1d(), well("50", "2").explanation());
    }

    @Test
    void theWellIgnoresOmegaAndReportsNone() {
        LevelsController.LevelsResult r =
            controller.levels("well", "0", "1", "50", "2", "abc");

        assertNull(r.omega());
    }

    @Test
    void theOscillatorNeedsOmega() {
        String m = message(() -> controller.levels("harmonic", "-8", "8", "100", "2", null));

        assertTrue(m.startsWith("omega "));
    }

    @Test
    void reportsAMissingParameterByName() {
        assertTrue(message(() -> controller.levels(null, "0", "1", "50", "2", null)).startsWith("potential "));
        assertTrue(message(() -> controller.levels("well", null, "1", "50", "2", null)).startsWith("xMin "));
        assertTrue(message(() -> controller.levels("well", "0", null, "50", "2", null)).startsWith("xMax "));
        assertTrue(message(() -> controller.levels("well", "0", "1", null, "2", null)).startsWith("points "));
        assertTrue(message(() -> controller.levels("well", "0", "1", "50", null, null)).startsWith("levels "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"gaussian", "", "wells"})
    void rejectsAnUnknownPotential(String potential) {
        assertTrue(
            message(() -> controller.levels(potential, "0", "1", "50", "2", null))
                .startsWith("potential "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"9", "20001", "abc", "1.5", ""})
    void rejectsAnInvalidNumberOfPoints(String points) {
        assertTrue(
            message(() -> controller.levels("well", "0", "1", points, "2", null))
                .startsWith("points "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "21", "abc", ""})
    void rejectsAnInvalidNumberOfLevels(String levels) {
        assertTrue(
            message(() -> controller.levels("well", "0", "1", "100", levels, null))
                .startsWith("levels "));
    }

    @Test
    void cannotAskForMoreLevelsThanGridPoints() {
        assertDoesNotThrow(() -> controller.levels("well", "0", "1", "10", "10", null));
        assertTrue(
            message(() -> controller.levels("well", "0", "1", "10", "11", null))
                .startsWith("levels "));
    }

    @Test
    void theRightWallMustBeToTheRightOfTheLeftWall() {
        assertTrue(message(() -> controller.levels("well", "1", "1", "50", "2", null)).startsWith("xMax "));
        assertTrue(message(() -> controller.levels("well", "2", "1", "50", "2", null)).startsWith("xMax "));
    }

    @Test
    void rejectsWallsBeyondTheLimit() {
        assertTrue(
            message(() -> controller.levels("well", "-1000001", "1", "50", "2", null))
                .startsWith("xMin and xMax "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "1000001"})
    void rejectsAnInvalidOmega(String omega) {
        assertTrue(
            message(() -> controller.levels("harmonic", "-8", "8", "100", "2", omega))
                .startsWith("omega "));
    }
}
