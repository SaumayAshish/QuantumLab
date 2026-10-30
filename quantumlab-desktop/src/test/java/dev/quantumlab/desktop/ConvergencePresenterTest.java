package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.desktop.SchrodingerPresenter.PotentialKind;
import dev.quantumlab.simulation.SeriesPoint;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConvergencePresenterTest {

    private final ConvergencePresenter presenter = new ConvergencePresenter();

    private static ConvergencePresenter.Request well() {
        return new ConvergencePresenter.Request(
            PotentialKind.INFINITE_WELL, "0", "1", "1", "1", "15", "6");
    }

    private static ConvergencePresenter.Request oscillator(String xMin, String xMax) {
        return new ConvergencePresenter.Request(PotentialKind.HARMONIC, xMin, xMax, "1", "0", "99", "4");
    }

    private static ConvergencePresenter.Request with(
        ConvergencePresenter.Request r, String level, String first, String steps) {
        return new ConvergencePresenter.Request(r.potential(), r.xMin(), r.xMax(), r.omega(), level, first, steps);
    }

    private static String messageOf(ConvergencePresenter.Request r, ConvergencePresenter p) {
        return assertThrows(InvalidInputException.class, () -> p.study(r)).getMessage();
    }

    @Test
    @DisplayName("the well study lists six grids, halving h each time, against the exact energy")
    void wellRows() {
        ConvergencePresenter.Result r = presenter.study(well());
        assertEquals("Infinite square well, n = 1", r.title());
        assertEquals("4.934802", r.reference());
        assertEquals(
            List.of("15", "31", "63", "127", "255", "511"),
            r.rows().stream().map(ConvergencePresenter.RowView::points).toList());
        assertEquals("0.062500", r.rows().get(0).spacing());
        assertEquals("4.91896822", r.rows().get(0).numerical());
        assertEquals("−1.583e-02", r.rows().get(0).error());
        assertEquals("–", r.rows().get(0).order());
        assertEquals("1.9986", r.rows().get(1).order());
        assertEquals("2.0000", r.rows().get(5).order());
    }

    @Test
    @DisplayName("the plotted errors lie on a line of slope 2 and the guide has slope 2")
    void slope() {
        ConvergencePresenter.Result r = presenter.study(well());
        assertEquals(6, r.errors().size());
        for (int k = 1; k < r.errors().size(); k++) {
            SeriesPoint a = r.errors().get(k - 1);
            SeriesPoint b = r.errors().get(k);
            assertTrue(b.x() < a.x(), "h shrinks");
            assertEquals(2.0, (b.y() - a.y()) / (b.x() - a.x()), 0.01);
        }
        assertEquals(2, r.guide().size());
        SeriesPoint g0 = r.guide().get(0);
        SeriesPoint g1 = r.guide().get(1);
        assertEquals(2.0, (g1.y() - g0.y()) / (g1.x() - g0.x()), 1e-12);
        assertEquals(r.errors().get(0), g0);
    }

    @Test
    @DisplayName("the summary reports the observed order at the finest refinement")
    void summary() {
        String s = presenter.study(well()).summary();
        assertTrue(s.contains("Observed order at the finest refinement: 2.0000"));
        assertTrue(s.contains("second order"));
    }

    @Test
    @DisplayName("a wide oscillator box converges at order 2 and says nothing about wall domination")
    void wideOscillator() {
        ConvergencePresenter.Result r = presenter.study(oscillator("-10", "10"));
        assertEquals("Harmonic oscillator (ω = 1.0000), n = 0", r.title());
        assertEquals("0.500000", r.reference());
        assertEquals("2.0002", r.rows().get(3).order());
        assertTrue(!r.summary().contains("wall error"));
        assertTrue(r.note().contains("walls"));
    }

    @Test
    @DisplayName("a narrow oscillator box stops converging and the summary says why")
    void narrowOscillator() {
        ConvergencePresenter.Result r = presenter.study(oscillator("-3", "3"));
        assertTrue(r.rows().get(3).error().startsWith("3.89"));
        assertTrue(r.rows().get(3).order().startsWith("−0.0"), r.rows().get(3).order());
        assertTrue(r.summary().contains("well below 2"));
        assertTrue(r.summary().contains("wall error"));
    }

    @Test
    @DisplayName("the well's levels count from 1 and the oscillator's from 0")
    void levelRanges() {
        assertTrue(messageOf(with(well(), "0", "15", "6"), presenter).contains("Level n"));
        assertTrue(messageOf(with(well(), "21", "15", "6"), presenter).contains("between 1 and 20"));
        ConvergencePresenter.Request osc = oscillator("-10", "10");
        assertTrue(messageOf(with(osc, "-1", "99", "4"), presenter).contains("between 0 and 19"));
        assertEquals("Infinite square well, n = 3", presenter.study(with(well(), "3", "15", "4")).title());
    }

    @Test
    @DisplayName("the grid and refinement settings are validated, including the finest grid size")
    void gridValidation() {
        assertTrue(messageOf(with(well(), "1", "5", "6"), presenter).contains("First grid size"));
        assertTrue(messageOf(with(well(), "1", "15", "1"), presenter).contains("Number of refinements"));
        assertTrue(messageOf(with(well(), "1", "15", "11"), presenter).contains("Number of refinements"));
        assertTrue(messageOf(with(well(), "1", "15.5", "6"), presenter).contains("whole number"));
        String tooBig = messageOf(with(well(), "1", "5000", "6"), presenter);
        assertTrue(tooBig.contains("finest grid would have"));
        assertTrue(tooBig.contains("20000"));
        assertTrue(
            messageOf(with(well(), "16", "15", "4"), presenter).contains("more points than the level index"));
    }

    @Test
    @DisplayName("walls and frequency are validated; the frequency is ignored for the well")
    void physicsValidation() {
        assertTrue(
            messageOf(
                new ConvergencePresenter.Request(
                    PotentialKind.INFINITE_WELL, "1", "1", "1", "1", "15", "4"),
                presenter)
                .contains("right of the left wall"));
        assertTrue(
            messageOf(
                new ConvergencePresenter.Request(
                    PotentialKind.INFINITE_WELL, "x", "1", "1", "1", "15", "4"),
                presenter)
                .contains("Left wall"));
        assertTrue(
            messageOf(
                new ConvergencePresenter.Request(
                    PotentialKind.HARMONIC, "-10", "10", "0", "0", "99", "4"),
                presenter)
                .contains("positive"));
        presenter.study(
            new ConvergencePresenter.Request(
                PotentialKind.INFINITE_WELL, "0", "1", "garbage", "1", "15", "4"));
        assertThrows(InvalidInputException.class, () -> presenter.study(null));
    }

    @Test
    @DisplayName("scientific notation uses a true minus sign and keeps the exponent's hyphen")
    void scientific() {
        assertEquals("−1.583e-02", ConvergencePresenter.scientific(-0.015830));
        assertEquals("3.893e-04", ConvergencePresenter.scientific(0.00038931));
        assertEquals("0.000e+00", ConvergencePresenter.scientific(0.0));
    }
}
