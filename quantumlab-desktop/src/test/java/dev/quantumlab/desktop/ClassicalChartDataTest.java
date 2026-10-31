package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.simulation.ClassicalDecayConfig;
import dev.quantumlab.simulation.ClassicalDecaySession;
import dev.quantumlab.simulation.SeriesPoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClassicalChartDataTest {

    private static final double LN2 = Math.log(2.0); // half-life 1, so both outcomes have P = 1/2

    private static ClassicalDecaySession session() {
        return new ClassicalDecaySession(new ClassicalDecayConfig(LN2, 1.0, 12345L));
    }

    @Test
    void labelsAndTheoryBeforeAnyRun() {
        ChartData d = ChartData.ofClassical(session(), 300);

        assertEquals(List.of("not decayed", "decayed"), d.labels());
        assertEquals(0.5, d.theoretical().get(0), 1e-12);
        assertEquals(0.5, d.theoretical().get(1), 1e-12);
        assertEquals("not decayed", d.convergenceLabel());
        assertEquals(0.5, d.convergenceTheory(), 1e-12);
        assertTrue(d.convergence().isEmpty());
    }

    @Test
    void observedFrequenciesComeFromTheCounts() {
        ClassicalDecaySession s = session();
        s.run(1000);
        long[] counts = s.counts();

        ChartData d = ChartData.ofClassical(s, 300);

        assertEquals(counts[0] / 1000.0, d.observed().get(0), 1e-12);
        assertEquals(counts[1] / 1000.0, d.observed().get(1), 1e-12);
        assertEquals(1.0, d.observed().get(0) + d.observed().get(1), 1e-12);
    }

    @Test
    void theRunningFrequencyEndsAtTheOverallFrequency() {
        ClassicalDecaySession s = session();
        s.run(1000);

        ChartData d = ChartData.ofClassical(s, 300);
        List<SeriesPoint> curve = d.convergence();

        assertTrue(curve.size() >= 2 && curve.size() <= 300, "points: " + curve.size());
        SeriesPoint last = curve.get(curve.size() - 1);
        assertEquals(1000.0, last.x(), 0.0);
        assertEquals(d.observed().get(0), last.y(), 1e-12);
    }

    @Test
    void theSameSeedGivesTheSameChartData() {
        ClassicalDecaySession a = session();
        ClassicalDecaySession b = session();
        a.run(500);
        b.run(500);

        assertEquals(ChartData.ofClassical(a, 300), ChartData.ofClassical(b, 300));
    }

    @Test
    void resetEmptiesTheConvergenceCurve() {
        ClassicalDecaySession s = session();
        s.run(100);
        assertFalse(ChartData.ofClassical(s, 300).convergence().isEmpty());

        s.reset();

        assertTrue(ChartData.ofClassical(s, 300).convergence().isEmpty());
    }

    @Test
    void aChartNeedsAtLeastTwoPoints() {
        assertThrows(IllegalArgumentException.class, () -> ChartData.ofClassical(session(), 1));
    }

    @Test
    void thePresenterNeedsAnExperimentFirst() {
        ClassicalPresenter presenter = new ClassicalPresenter();
        assertThrows(IllegalStateException.class, () -> presenter.chartData(300));
    }

    @Test
    void thePresenterChartFollowsTheRuns() {
        ClassicalPresenter presenter = new ClassicalPresenter();
        presenter.prepare(new ClassicalPresenter.Request("0.693147", "1", "12345"));
        assertTrue(presenter.chartData(300).convergence().isEmpty());

        presenter.run("1000");
        ChartData d = presenter.chartData(300);

        assertEquals(List.of("not decayed", "decayed"), d.labels());
        assertEquals(1.0, d.observed().get(0) + d.observed().get(1), 1e-12);
        assertFalse(d.convergence().isEmpty());
    }
}
