package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.desktop.ClassicalPresenter.ApparatusView;
import dev.quantumlab.desktop.ClassicalPresenter.Request;
import dev.quantumlab.simulation.ClassicalDecaySession;
import dev.quantumlab.simulation.ExperimentExporter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClassicalPresenterTest {

    private static final String LN2 = Double.toString(Math.log(2.0));

    private static ClassicalPresenter prepared(String rate, String time, String seed) {
        ClassicalPresenter p = new ClassicalPresenter();
        p.prepare(new Request(rate, time, seed));
        return p;
    }

    @Test
    @DisplayName("the setup view shows the decay law: half-life 1 gives 50 % decayed at t = 1")
    void setupView() {
        ClassicalPresenter.SetupView v =
                new ClassicalPresenter().prepare(new Request(LN2, "1", "1"));
        assertEquals("1.0000", v.halfLife());
        assertEquals("50.00 %", v.decayProbability());
        assertEquals("50.00 %", v.survivalProbability());
        assertEquals("0.6931", v.decayRate());
        assertEquals("1.0000", v.observationTime());
    }

    @Test
    @DisplayName("a stable nucleus has an infinite half-life and never decays")
    void stableNucleus() {
        ClassicalPresenter.SetupView v = new ClassicalPresenter().prepare(new Request("0", "5", "1"));
        assertEquals("∞ (stable)", v.halfLife());
        assertEquals("0.00 %", v.decayProbability());
        assertEquals("100.00 %", v.survivalProbability());
    }

    @Test
    @DisplayName("invalid settings are rejected with messages that say what is wrong")
    void invalidInput() {
        ClassicalPresenter p = new ClassicalPresenter();
        InvalidInputException negativeRate =
                assertThrows(InvalidInputException.class, () -> p.prepare(new Request("-1", "1", "1")));
        assertTrue(negativeRate.getMessage().contains("Decay rate"));
        InvalidInputException negativeTime =
                assertThrows(InvalidInputException.class, () -> p.prepare(new Request("1", "-1", "1")));
        assertTrue(negativeTime.getMessage().contains("Observation time"));
        assertThrows(InvalidInputException.class, () -> p.prepare(new Request("abc", "1", "1")));
        assertThrows(InvalidInputException.class, () -> p.prepare(new Request("1", "", "1")));
        assertThrows(InvalidInputException.class, () -> p.prepare(new Request("1", "1", "")));
        assertThrows(InvalidInputException.class, () -> p.prepare(new Request("NaN", "1", "1")));
        assertThrows(InvalidInputException.class, () -> p.prepare(null));
        assertFalse(p.hasExperiment());
    }

    @Test
    @DisplayName("a failed prepare leaves the running experiment untouched")
    void failedPrepareKeepsPrevious() {
        ClassicalPresenter p = prepared(LN2, "1", "5");
        p.run("10");
        assertThrows(InvalidInputException.class, () -> p.prepare(new Request("x", "1", "1")));
        assertEquals(10, p.results().trials());
    }

    @Test
    @DisplayName("everything that needs an experiment says so before one exists")
    void requiresExperiment() {
        ClassicalPresenter p = new ClassicalPresenter();
        assertThrows(IllegalStateException.class, p::runOnce);
        assertThrows(IllegalStateException.class, () -> p.run("1"));
        assertThrows(IllegalStateException.class, p::reset);
        assertThrows(IllegalStateException.class, p::results);
        assertThrows(IllegalStateException.class, p::apparatus);
        assertThrows(IllegalStateException.class, p::lastDraw);
        assertThrows(IllegalStateException.class, p::exportJson);
    }

    @Test
    @DisplayName("before any run the apparatus is at rest and no draw has happened")
    void initialApparatus() {
        ClassicalPresenter p = prepared(LN2, "1", "1");
        assertEquals(new ApparatusView("intact", "no count", "armed", "alive"), p.apparatus());
        assertEquals("No run yet.", p.lastDraw());
    }

    @Test
    @DisplayName("a certain decay runs the whole chain: nucleus, detector, trigger, cat")
    void certainDecay() {
        ClassicalPresenter p = prepared("1000", "1000", "1");
        assertEquals(new ApparatusView("decayed", "count registered", "released", "dead"), p.runOnce());
        assertTrue(p.lastDraw().contains(" < "));
        assertTrue(p.lastDraw().endsWith("nucleus decayed"));
    }

    @Test
    @DisplayName("a stable nucleus leaves the cat alive however many runs there are")
    void certainSurvival() {
        ClassicalPresenter p = prepared("0", "1", "1");
        for (int i = 0; i < 50; i++) {
            assertEquals(new ApparatusView("intact", "no count", "armed", "alive"), p.runOnce());
        }
        assertTrue(p.lastDraw().contains(" ≥ "));
        assertTrue(p.lastDraw().endsWith("nucleus intact"));
    }

    @Test
    @DisplayName("in every run the four links agree: the cat is dead exactly when the nucleus decayed")
    void chainIsConsistent() {
        ClassicalPresenter p = prepared(LN2, "1", "99");
        for (int i = 0; i < 300; i++) {
            ApparatusView v = p.runOnce();
            boolean decayed = v.nucleus().equals("decayed");
            assertEquals(decayed, v.detector().equals("count registered"));
            assertEquals(decayed, v.trigger().equals("released"));
            assertEquals(decayed, v.cat().equals("dead"));
        }
        assertEquals(300, p.results().trials());
    }

    @Test
    @DisplayName("repeated runs agree with the decay law within statistical error")
    void statistics() {
        ClassicalPresenter p = prepared(LN2, "1", "42");
        ResultsView r = p.run("10000");
        assertEquals(10_000, r.trials());
        assertEquals(List.of("not decayed", "decayed"), r.rows().stream().map(ResultsView.Row::label).toList());
        long total = 0;
        for (ResultsView.Row row : r.rows()) {
            total += Long.parseLong(row.count());
        }
        assertEquals(10_000L, total);
        assertEquals("50.00 %", r.rows().get(1).theoretical());
        assertTrue(r.verdict().startsWith("All outcomes are within 5σ"));
    }

    @Test
    @DisplayName("the same seed gives identical results, and reset replays them exactly")
    void deterministicAndResettable() {
        ClassicalPresenter a = prepared("0.3", "2", "777");
        ClassicalPresenter b = prepared("0.3", "2", "777");
        ResultsView first = a.run("500");
        assertEquals(first, b.run("500"));
        assertEquals(0, a.reset().trials());
        assertEquals(first, a.run("500"));
    }

    @Test
    @DisplayName("the run limit is enforced")
    void runLimit() {
        ClassicalPresenter p = prepared(LN2, "1", "3");
        ResultsView r = p.run(Integer.toString(ClassicalDecaySession.MAX_RUNS));
        assertEquals(0, r.remaining());
        assertThrows(InvalidInputException.class, () -> p.run("1"));
        assertThrows(InvalidInputException.class, p::runOnce);
    }

    @Test
    @DisplayName("after a batch, the apparatus and the draw line describe the last run")
    void batchLeavesLastRun() {
        ClassicalPresenter p = prepared(LN2, "1", "5");
        p.run("5");
        assertFalse(p.lastDraw().equals("No run yet."));
        ApparatusView v = p.apparatus();
        assertEquals(v.nucleus().equals("decayed"), p.lastDraw().endsWith("nucleus decayed"));
    }

    @Test
    @DisplayName("the export is the classical schema and carries the seed that was typed in")
    void export() {
        ClassicalPresenter p = prepared(LN2, "1", "7");
        p.run("10");
        String json = p.exportJson();
        assertTrue(json.contains("\"schema\": \"" + ExperimentExporter.SCHEMA_CLASSICAL + "\""));
        assertTrue(json.contains("\"mode\": \"classical-decay\""));
        assertTrue(json.contains("\"seed\": 7"));
    }
}
