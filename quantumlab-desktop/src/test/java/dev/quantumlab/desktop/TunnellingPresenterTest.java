package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.desktop.TunnellingPresenter.BarrierKind;
import dev.quantumlab.simulation.SeriesPoint;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TunnellingPresenterTest {

    private final TunnellingPresenter presenter = new TunnellingPresenter();

    private static TunnellingPresenter.Request single() {
        return new TunnellingPresenter.Request(BarrierKind.SINGLE, "2", "1.5", "1", "4", "1");
    }

    private static TunnellingPresenter.Request doubleBarrier() {
        return new TunnellingPresenter.Request(BarrierKind.DOUBLE, "5", "0.5", "1", "8", "2");
    }

    private static String messageOf(TunnellingPresenter.Request r, TunnellingPresenter p) {
        return assertThrows(InvalidInputException.class, () -> p.compute(r)).getMessage();
    }

    @Test
    @DisplayName("a single barrier lists seven energies from 0.25 V0 to 2 V0 against the classical answer")
    void singleRows() {
        TunnellingPresenter.Result r = presenter.compute(single());
        assertEquals("Single barrier: height V0 = 2.0000, width a = 1.5000", r.setup());
        assertEquals(7, r.rows().size());
        TunnellingPresenter.RowView half = r.rows().get(1);
        assertEquals("E = 0.50 V0", half.fraction());
        assertEquals("1.0000", half.energy());
        assertEquals("below the barrier", half.regime());
        assertEquals("0", half.classical());
        assertEquals("0.055861", half.transmission());
        assertEquals("0.944139", half.reflection());
        assertEquals("at the top", r.rows().get(3).regime());
        assertEquals("–", r.rows().get(3).classical());
        assertEquals("above the barrier", r.rows().get(4).regime());
        assertEquals("1", r.rows().get(4).classical());
        assertEquals("0.997517", r.rows().get(6).transmission());
        for (TunnellingPresenter.RowView row : r.rows()) {
            assertEquals(row.transmission(), row.closedForm(), "closed form agrees at " + row.fraction());
        }
    }

    @Test
    @DisplayName("the probe reports T, R, the decay constant and the thick-barrier estimate")
    void singleProbe() {
        TunnellingPresenter.ProbeView p = presenter.compute(single()).probeView();
        assertEquals("1.0000", p.energy());
        assertEquals("below the barrier", p.regime());
        assertEquals("0.055861", p.transmission());
        assertEquals("0.944139", p.reflection());
        assertEquals("1.4142", p.decayConstant());
        assertEquals("0.057478", p.thickEstimate());
    }

    @Test
    @DisplayName("above the barrier the probe has no decay constant or estimate")
    void probeAbove() {
        TunnellingPresenter.Result r =
            presenter.compute(
                new TunnellingPresenter.Request(BarrierKind.SINGLE, "2", "1.5", "1", "4", "3"));
        assertEquals("above the barrier", r.probeView().regime());
        assertEquals("0.805081", r.probeView().transmission());
        assertEquals("–", r.probeView().decayConstant());
        assertEquals("–", r.probeView().thickEstimate());
    }

    @Test
    @DisplayName("the double barrier matches the NumPy reference and shows a resonance below the top")
    void doubleBarrierResonance() {
        TunnellingPresenter.Result r = presenter.compute(doubleBarrier());
        assertTrue(r.setup().startsWith("Double barrier: two barriers of height V0 = 5.0000"));
        assertTrue(r.setup().contains("gap of 1.0000"));
        assertEquals("0.300545", r.probeView().transmission());
        assertEquals("–", r.probeView().thickEstimate());
        assertTrue(r.peak().contains("T = 0.996025"), r.peak());
        assertTrue(r.peak().contains("E ≈ 1.7000"), r.peak());
        assertTrue(r.note().contains("resonant tunnelling"));
        for (TunnellingPresenter.RowView row : r.rows()) {
            assertEquals("–", row.closedForm());
        }
    }

    @Test
    @DisplayName("the curve has 400 energies up to the maximum, all probabilities")
    void curve() {
        TunnellingPresenter.Result r = presenter.compute(single());
        assertEquals(400, r.curve().size());
        assertEquals(0.01, r.curve().get(0).x(), 1e-12);
        assertEquals(4.0, r.curve().get(399).x(), 1e-12);
        for (int k = 0; k < 400; k++) {
            SeriesPoint p = r.curve().get(k);
            assertTrue(p.y() >= 0.0 && p.y() <= 1.0 + 1e-12, "T=" + p.y());
            if (k > 0) {
                assertTrue(p.x() > r.curve().get(k - 1).x());
            }
        }
        assertEquals(0.9975168244263539, r.curve().get(399).y(), 1e-12);
    }

    @Test
    @DisplayName("the barrier-top and probe lines are drawn only when they lie inside the plotted range")
    void markers() {
        TunnellingPresenter.Result r = presenter.compute(single());
        assertEquals(List.of(new SeriesPoint(2.0, 0.0), new SeriesPoint(2.0, 1.0)), r.barrierTop());
        assertEquals(2, r.probe().size());
        assertEquals(new SeriesPoint(1.0, 0.0), r.probe().get(0));
        assertEquals(0.055861437216915, r.probe().get(1).y(), 1e-12);

        TunnellingPresenter.Result narrow =
            presenter.compute(
                new TunnellingPresenter.Request(BarrierKind.SINGLE, "2", "1.5", "1", "1.5", "1.8"));
        assertTrue(narrow.barrierTop().isEmpty());
        assertTrue(narrow.probe().isEmpty());
        assertEquals("0.141942", presenter.compute(
                new TunnellingPresenter.Request(BarrierKind.SINGLE, "2", "1.5", "1", "1.5", "1.5"))
            .probeView().transmission());
    }

    @Test
    @DisplayName("an extremely opaque barrier shows 0 and never NaN")
    void veryOpaque() {
        TunnellingPresenter.Result r =
            presenter.compute(
                new TunnellingPresenter.Request(BarrierKind.SINGLE, "1000", "100", "1", "2000", "1"));
        assertEquals("0", r.probeView().transmission());
        assertEquals("1.000000", r.probeView().reflection());
        assertEquals("0", r.rows().get(0).transmission());
        assertEquals("0", r.rows().get(0).closedForm());
        for (TunnellingPresenter.RowView row : r.rows()) {
            assertFalse(row.transmission().contains("NaN") || row.closedForm().contains("NaN"));
        }
        for (SeriesPoint p : r.curve()) {
            assertFalse(Double.isNaN(p.y()));
        }
    }

    @Test
    @DisplayName("small probabilities use scientific notation instead of rounding to 0.000000")
    void probabilityFormat() {
        assertEquals("0", TunnellingPresenter.probability(0.0));
        assertEquals("2.000e-07", TunnellingPresenter.probability(2.0e-7));
        assertEquals("0.000100", TunnellingPresenter.probability(1.0e-4));
        assertEquals("9.900e-05", TunnellingPresenter.probability(9.9e-5));
        assertEquals("0.500000", TunnellingPresenter.probability(0.5));
        assertEquals("1.000000", TunnellingPresenter.probability(1.0));
    }

    @Test
    @DisplayName("invalid settings are rejected with plain messages; the gap is only read for two barriers")
    void validation() {
        assertTrue(messageOf(new TunnellingPresenter.Request(BarrierKind.SINGLE, "0", "1.5", "1", "4", "1"), presenter)
            .contains("Barrier height V0"));
        assertTrue(messageOf(new TunnellingPresenter.Request(BarrierKind.SINGLE, "2", "-1", "1", "4", "1"), presenter)
            .contains("Barrier width a"));
        assertTrue(messageOf(new TunnellingPresenter.Request(BarrierKind.SINGLE, "2", "1.5", "1", "0", "1"), presenter)
            .contains("Maximum energy"));
        assertTrue(messageOf(new TunnellingPresenter.Request(BarrierKind.SINGLE, "2", "1.5", "1", "4", "abc"), presenter)
            .contains("Probe energy E"));
        assertTrue(messageOf(new TunnellingPresenter.Request(BarrierKind.SINGLE, "2000", "1.5", "1", "4", "1"), presenter)
            .contains("at most 1000"));
        assertTrue(messageOf(new TunnellingPresenter.Request(BarrierKind.DOUBLE, "5", "0.5", "x", "8", "2"), presenter)
            .contains("Gap between the barriers"));
        presenter.compute(new TunnellingPresenter.Request(BarrierKind.SINGLE, "2", "1.5", "garbage", "4", "1"));
        assertThrows(InvalidInputException.class, () -> presenter.compute(null));
        assertThrows(
            InvalidInputException.class,
            () -> presenter.compute(new TunnellingPresenter.Request(null, "2", "1.5", "1", "4", "1")));
    }
}
