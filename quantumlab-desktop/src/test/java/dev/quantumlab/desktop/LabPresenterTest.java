package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.desktop.LabPresenter.Request;
import dev.quantumlab.physics.QubitState;
import dev.quantumlab.physics.StandardBasis;
import dev.quantumlab.simulation.ExperimentExporter;
import dev.quantumlab.simulation.Formatting;
import dev.quantumlab.simulation.MeasurementSession;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LabPresenterTest {

    private static Request request(QubitState s, StandardBasis b, String seed) {
        return Request.fromState(s, b, seed);
    }

    private static LabPresenter prepared(QubitState s, StandardBasis b, String seed) {
        LabPresenter p = new LabPresenter();
        p.prepare(request(s, b, seed));
        return p;
    }

    @Test
    @DisplayName("preparing |+> shows the state, a 50/50 split in Z, and certainty in the Hadamard basis")
    void prepareView() {
        LabPresenter p = new LabPresenter();
        StateView v = p.prepare(request(QubitState.plus(), StandardBasis.HADAMARD, "1"));
        assertEquals("50.00 %", v.probability0());
        assertEquals("50.00 %", v.probability1());
        assertEquals(List.of("|+⟩", "|−⟩"), v.basisLabels());
        assertEquals(List.of("100.00 %", "0.00 %"), v.basisProbabilities());
        assertEquals("", v.note());
        assertTrue(p.hasExperiment());
    }

    @Test
    @DisplayName("the displayed amplitudes match the prepared state")
    void amplitudesDisplayed() {
        QubitState s = QubitState.fromBloch(1.2, 0.8);
        StateView v = new LabPresenter().prepare(request(s, StandardBasis.COMPUTATIONAL, "1"));
        assertEquals(Formatting.complex(s.alpha(), 4), v.alpha());
        assertEquals(Formatting.complex(s.beta(), 4), v.beta());
    }

    @Test
    @DisplayName("amplitudes that are not normalised are rescaled, and the user is told")
    void renormalisationIsDisclosed() {
        StateView v = new LabPresenter().prepare(new Request("1", "0", "1", "0", StandardBasis.COMPUTATIONAL, "1"));
        assertEquals("50.00 %", v.probability0());
        assertTrue(v.note().contains("rescaled"));
        assertTrue(v.note().contains("2.0000"));
    }

    @Test
    @DisplayName("invalid input is rejected with a message that names the field")
    void invalidInput() {
        LabPresenter p = new LabPresenter();
        InvalidInputException e =
                assertThrows(
                        InvalidInputException.class,
                        () -> p.prepare(new Request("", "0", "1", "0", StandardBasis.COMPUTATIONAL, "1")));
        assertTrue(e.getMessage().contains("α (real part)"));

        assertThrows(InvalidInputException.class, () -> p.prepare(new Request("NaN", "0", "1", "0", StandardBasis.COMPUTATIONAL, "1")));
        assertThrows(InvalidInputException.class, () -> p.prepare(new Request("1", "0", "0", "0", StandardBasis.COMPUTATIONAL, "abc")));
        assertThrows(InvalidInputException.class, () -> p.prepare(new Request("1", "0", "0", "0", null, "1")));
        assertThrows(InvalidInputException.class, () -> p.prepare(null));
        assertFalse(p.hasExperiment());
    }

    @Test
    @DisplayName("two zero amplitudes are rejected, and so are amplitudes too large to square")
    void degenerateAmplitudes() {
        LabPresenter p = new LabPresenter();
        InvalidInputException zero =
                assertThrows(InvalidInputException.class, () -> p.prepare(new Request("0", "0", "0", "0", StandardBasis.COMPUTATIONAL, "1")));
        assertTrue(zero.getMessage().contains("zero"));
        assertThrows(InvalidInputException.class, () -> p.prepare(new Request("1e200", "0", "1e200", "0", StandardBasis.COMPUTATIONAL, "1")));
        assertThrows(InvalidInputException.class, () -> p.prepare(new Request("1e-200", "0", "1e-200", "0", StandardBasis.COMPUTATIONAL, "1")));
    }

    @Test
    @DisplayName("a failed prepare leaves the running experiment untouched")
    void failedPrepareKeepsPrevious() {
        LabPresenter p = prepared(QubitState.plus(), StandardBasis.HADAMARD, "5");
        p.measure("10");
        assertThrows(InvalidInputException.class, () -> p.prepare(new Request("x", "0", "1", "0", StandardBasis.COMPUTATIONAL, "1")));
        assertEquals(10, p.results().trials());
    }

    @Test
    @DisplayName("running or reading results before preparing is an error, not a crash later")
    void requiresExperiment() {
        LabPresenter p = new LabPresenter();
        assertThrows(IllegalStateException.class, () -> p.measure("1"));
        assertThrows(IllegalStateException.class, p::results);
        assertThrows(IllegalStateException.class, p::reset);
        assertThrows(IllegalStateException.class, () -> p.history(5));
        assertThrows(IllegalStateException.class, p::exportJson);
        assertThrows(IllegalStateException.class, p::exportCsv);
    }

    @Test
    @DisplayName("before any trial, results are empty and say so")
    void emptyResults() {
        ResultsView r = prepared(QubitState.plus(), StandardBasis.COMPUTATIONAL, "1").results();
        assertEquals(0, r.trials());
        assertEquals("No trials yet.", r.verdict());
        assertEquals(2, r.rows().size());
        assertEquals("0", r.rows().get(0).count());
        assertEquals("0.00 %", r.rows().get(0).observed());
    }

    @Test
    @DisplayName("results add up and agree with the Born rule within statistical error")
    void statistics() {
        LabPresenter p = prepared(QubitState.fromBloch(1.2, 0.8), StandardBasis.HADAMARD, "42");
        ResultsView r = p.measure("10000");
        assertEquals(10_000, r.trials());
        assertEquals(MeasurementSession.MAX_TRIALS - 10_000, r.remaining());
        long total = 0;
        for (ResultsView.Row row : r.rows()) {
            total += Long.parseLong(row.count());
        }
        assertEquals(10_000L, total);
        assertTrue(r.verdict().startsWith("All outcomes are within 5σ"));
    }

    @Test
    @DisplayName("the same seed gives identical results, and reset replays them exactly")
    void deterministicAndResettable() {
        Request req = request(QubitState.fromBloch(1.2, 0.8), StandardBasis.CIRCULAR, "777");
        LabPresenter a = new LabPresenter();
        LabPresenter b = new LabPresenter();
        a.prepare(req);
        b.prepare(req);
        ResultsView first = a.measure("500");
        assertEquals(first, b.measure("500"));

        ResultsView cleared = a.reset();
        assertEquals(0, cleared.trials());
        assertEquals(first, a.measure("500"));
    }

    @Test
    @DisplayName("the trial limit is enforced, and a run that crosses it stops at the limit")
    void trialLimit() {
        LabPresenter p = prepared(QubitState.plus(), StandardBasis.COMPUTATIONAL, "3");
        p.measure(Integer.toString(MeasurementSession.MAX_TRIALS - 1));
        ResultsView r = p.measure("5");
        assertEquals(MeasurementSession.MAX_TRIALS, r.trials());
        assertEquals(0, r.remaining());
        assertThrows(InvalidInputException.class, () -> p.measure("1"));
        assertEquals(0, p.reset().trials());
        assertEquals(MeasurementSession.MAX_TRIALS, p.results().remaining());
    }

    @Test
    @DisplayName("history lists the most recent trials, oldest first")
    void history() {
        LabPresenter p = prepared(QubitState.basis0(), StandardBasis.COMPUTATIONAL, "1");
        p.measure("3");
        assertEquals(
                List.of("#2  |0⟩  (p = 100.00 %)", "#3  |0⟩  (p = 100.00 %)"),
                p.history(2));
        assertEquals(3, p.history(100).size());
    }

    @Test
    @DisplayName("exports carry the schema and the seed that was typed in")
    void exports() {
        LabPresenter p = prepared(QubitState.plus(), StandardBasis.HADAMARD, "12345");
        p.measure("4");
        String json = p.exportJson();
        assertTrue(json.contains("\"schema\": \"" + ExperimentExporter.SCHEMA_MEASUREMENT + "\""));
        assertTrue(json.contains("\"seed\": 12345"));
        assertEquals(5, p.exportCsv().split("\n").length); // header + 4 trials
    }
}
