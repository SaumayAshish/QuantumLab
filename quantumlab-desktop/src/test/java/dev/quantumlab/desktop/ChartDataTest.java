package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.physics.QubitState;
import dev.quantumlab.physics.StandardBasis;
import dev.quantumlab.simulation.SeriesPoint;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChartDataTest {

    private static LabPresenter prepared(String seed) {
        LabPresenter p = new LabPresenter();
        p.prepare(
                LabPresenter.Request.fromState(
                        QubitState.fromBloch(1.2, 0.8), StandardBasis.COMPUTATIONAL, seed));
        return p;
    }

    @Test
    @DisplayName("before any trial the charts have theory but no observations or curve")
    void empty() {
        ChartData d = prepared("1").chartData(50);
        assertEquals(2, d.labels().size());
        assertEquals(List.of(0.0, 0.0), d.observed());
        assertEquals(1.0, d.theoretical().get(0) + d.theoretical().get(1), 1e-12);
        assertTrue(d.convergence().isEmpty());
    }

    @Test
    @DisplayName("frequencies and probabilities each sum to one, and match the results table")
    void sumsToOne() {
        LabPresenter p = prepared("7");
        ResultsView r = p.measure("1000");
        ChartData d = p.chartData(50);
        assertEquals(1.0, d.observed().get(0) + d.observed().get(1), 1e-12);
        assertEquals(1.0, d.theoretical().get(0) + d.theoretical().get(1), 1e-12);
        for (int k = 0; k < 2; k++) {
            assertEquals(Long.parseLong(r.rows().get(k).count()) / 1000.0, d.observed().get(k), 1e-12);
        }
    }

    @Test
    @DisplayName("the convergence curve is thinned, increasing, and ends at the observed frequency")
    void convergenceCurve() {
        LabPresenter p = prepared("42");
        p.measure("10000");
        ChartData d = p.chartData(50);
        List<SeriesPoint> c = d.convergence();
        assertTrue(c.size() <= 50 && c.size() >= 2);
        assertEquals(10_000.0, c.get(c.size() - 1).x(), 0.0);
        assertEquals(d.observed().get(0), c.get(c.size() - 1).y(), 1e-12);
        assertTrue(c.get(0).x() >= 1.0);
        for (int i = 1; i < c.size(); i++) {
            assertTrue(c.get(i).x() > c.get(i - 1).x(), "trial numbers must strictly increase");
        }
        for (SeriesPoint pt : c) {
            assertTrue(pt.y() >= 0.0 && pt.y() <= 1.0);
        }
    }

    @Test
    @DisplayName("with fewer trials than points, every trial is plotted")
    void shortRun() {
        LabPresenter p = prepared("3");
        p.measure("20");
        List<SeriesPoint> c = p.chartData(100).convergence();
        assertEquals(20, c.size());
        for (int i = 0; i < 20; i++) {
            assertEquals(i + 1.0, c.get(i).x(), 0.0);
        }
    }

    @Test
    @DisplayName("the plotted outcome is the first one, with its own theory value")
    void plottedOutcome() {
        LabPresenter p = prepared("3");
        p.measure("10");
        ChartData d = p.chartData(10);
        assertEquals(d.labels().get(0), d.convergenceLabel());
        assertEquals(d.theoretical().get(0), d.convergenceTheory(), 0.0);
    }

    @Test
    @DisplayName("the same seed gives identical chart data")
    void deterministic() {
        LabPresenter a = prepared("99");
        LabPresenter b = prepared("99");
        a.measure("3000");
        b.measure("3000");
        assertEquals(a.chartData(80), b.chartData(80));
    }

    @Test
    @DisplayName("a chart needs at least two points, and an experiment must exist")
    void invalidUse() {
        LabPresenter p = prepared("1");
        assertThrows(IllegalArgumentException.class, () -> p.chartData(1));
        assertThrows(IllegalStateException.class, () -> new LabPresenter().chartData(10));
    }
}
