package dev.quantumlab.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.physics.StandardBasis;
import dev.quantumlab.simulation.ExperimentExporter;
import dev.quantumlab.simulation.ExplanationCatalog;
import dev.quantumlab.simulation.MeasurementSession;
import dev.quantumlab.simulation.QuantumMeasurementConfig;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MeasurementControllerTest {

    private final MeasurementController controller = new MeasurementController();

    private String run(String p, String phase, String basis, String seed, String trials) {
        return controller.measurement(p, phase, basis, seed, trials);
    }

    private static boolean has(String json, String regex) {
        return Pattern.compile(regex).matcher(json).find();
    }

    @Test
    void theSameRequestGivesTheSameAnswer() {
        assertEquals(
            run("0.3", "0.5", "X", "99", "500"), run("0.3", "0.5", "X", "99", "500"));
    }

    @Test
    void givesWhatTheSessionAndExporterGiveForTheSameConfiguration() {
        double alpha = Math.sqrt(0.5);
        double beta = Math.sqrt(1.0 - 0.5);
        QuantumMeasurementConfig config =
            new QuantumMeasurementConfig(
                alpha, 0.0, beta * Math.cos(0.0), beta * Math.sin(0.0),
                StandardBasis.COMPUTATIONAL, 7L);
        MeasurementSession session = new MeasurementSession(config);
        session.measure(200);

        assertEquals(ExperimentExporter.toJson(session), run("0.5", null, "Z", "7", "200"));
    }

    @Test
    void carriesTheScopeNotesFromTheCatalogue() {
        String json = run("0.5", null, "Z", "1", "10");

        assertTrue(has(json, "\"scopeNotes\""));
        assertTrue(json.contains(ExplanationCatalog.general().doesNotRepresent().get(0)));
    }

    @Test
    void recordsTheNumberOfTrials() {
        assertTrue(has(run("0.5", null, "Z", "1", "1000"), "\"trials\"\\s*:\\s*1000"));
    }

    @Test
    void phaseDefaultsToZero() {
        assertEquals(
            run("0.5", "0", "X", "3", "100"), run("0.5", null, "X", "3", "100"));
    }

    @Test
    void marksALongHistoryAsTruncated() {
        int over = ExperimentExporter.MAX_JSON_HISTORY + 1;

        assertTrue(
            has(run("0.5", null, "Z", "1", String.valueOf(over)),
                "\"historyTruncated\"\\s*:\\s*true"));
        assertTrue(
            has(run("0.5", null, "Z", "1", "10"), "\"historyTruncated\"\\s*:\\s*false"));
    }

    @Test
    void reportsAMissingParameterByName() {
        assertTrue(missing(null, "Z", "1", "10").startsWith("p "));
        assertTrue(missing("0.5", null, "1", "10").startsWith("basis "));
        assertTrue(missing("0.5", "Z", null, "10").startsWith("seed "));
        assertTrue(missing("0.5", "Z", "1", null).startsWith("trials "));
    }

    private String missing(String p, String basis, String seed, String trials) {
        return assertThrows(
            BadRequestException.class, () -> run(p, null, basis, seed, trials))
            .getMessage();
    }

    @Test
    void acceptsTheThreeBasesInEitherCase() {
        for (String basis : new String[] {"Z", "x", "Y"}) {
            assertDoesNotThrow(() -> run("0.5", null, basis, "1", "10"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.5", "-0.1", "NaN", ""})
    void rejectsAnInvalidProbability(String p) {
        BadRequestException e =
            assertThrows(BadRequestException.class, () -> run(p, null, "Z", "1", "10"));
        assertTrue(e.getMessage().startsWith("p "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "100001", "abc", "1.5", ""})
    void rejectsAnInvalidTrialCount(String trials) {
        BadRequestException e =
            assertThrows(BadRequestException.class, () -> run("0.5", null, "Z", "1", trials));
        assertTrue(e.getMessage().startsWith("trials "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "", "99999999999999999999"})
    void rejectsAnInvalidSeed(String seed) {
        BadRequestException e =
            assertThrows(BadRequestException.class, () -> run("0.5", null, "Z", seed, "10"));
        assertTrue(e.getMessage().startsWith("seed "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Q", "", "COMPUTATIONAL"})
    void rejectsAnUnknownBasis(String basis) {
        BadRequestException e =
            assertThrows(BadRequestException.class, () -> run("0.5", null, basis, "1", "10"));
        assertTrue(e.getMessage().startsWith("basis "));
    }
}
