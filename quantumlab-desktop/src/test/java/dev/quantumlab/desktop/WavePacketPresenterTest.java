package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.desktop.WavePacketPresenter.Frame;
import dev.quantumlab.desktop.WavePacketPresenter.FrameText;
import dev.quantumlab.desktop.WavePacketPresenter.Request;
import dev.quantumlab.desktop.WavePacketPresenter.Result;
import dev.quantumlab.simulation.SeriesPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WavePacketPresenterTest {

    private final WavePacketPresenter presenter = new WavePacketPresenter();

    private static Request defaults() {
        return new Request("2", "1", "-20", "3", "1.5");
    }

    private static double number(String text) {
        return Double.parseDouble(text);
    }

    private static String messageOf(Request r, WavePacketPresenter p) {
        return assertThrows(InvalidInputException.class, () -> p.compute(r)).getMessage();
    }

    @Test
    @DisplayName("the default run stores 181 frames of 800 displayed points and conserves probability")
    void defaultRunStructure() {
        Result r = presenter.compute(defaults());
        assertEquals(WavePacketPresenter.FRAMES, r.frames().size());
        assertEquals(800, r.positions().length);
        assertEquals(36.0, r.endTime(), 1e-12);
        for (Frame f : r.frames()) {
            assertEquals(800, f.density().length);
            assertEquals(1.0, f.left() + f.inside() + f.right(), 1e-9);
        }
        Frame first = r.frames().get(0);
        assertEquals(0.0, first.time(), 0.0);
        assertTrue(first.left() > 0.999999999);
        assertTrue(first.right() < 1e-9);
        assertEquals(36.0, r.frames().get(180).time(), 1e-9);
        assertTrue(r.normCheck().contains("e-1"), "norm drift should be of order 1e-12 or smaller: " + r.normCheck());
    }

    @Test
    @DisplayName("the late-time split agrees with stationary theory averaged over the packet (and with the solver tests)")
    void defaultRunPhysics() {
        Result r = presenter.compute(defaults());
        assertEquals(0.250530, number(r.theoryAverage()), 1e-6);
        assertEquals(0.244528, number(r.theoryAtCentralEnergy()), 1e-6);
        assertEquals(0.249944, number(r.finalRight()), 1e-5);
        assertEquals(0.750056, number(r.finalLeft()), 1e-5);
        assertEquals(number(r.theoryAverage()), number(r.finalRight()), 2e-3);
        assertTrue(number(r.finalInside()) < 1e-6 || r.finalInside().contains("e-"));
        assertTrue(r.summary().contains("0.250530"));
        assertTrue(r.summary().contains("0.244528"));
    }

    @Test
    @DisplayName("a high, thick barrier and a barrier below the packet's energy also match the stationary average")
    void otherSettings() {
        // Reference values: stationary average integrated with NumPy (0.000263645 and 0.993804).
        Result tunnelling = presenter.compute(new Request("5", "2", "-25", "3", "2"));
        assertEquals(0.000263645, number(tunnelling.theoryAverage()), 5e-7); // shown with 6 decimals
        assertEquals(number(tunnelling.theoryAverage()), number(tunnelling.finalRight()), 0.05 * 0.000263645);

        Result above = presenter.compute(new Request("1", "1", "-20", "2", "3"));
        assertEquals(0.993804, number(above.theoryAverage()), 1e-6);
        assertEquals(0.993804, number(above.finalRight()), 1e-3);
    }

    @Test
    @DisplayName("the barrier width is rounded to whole grid cells and the setup says so")
    void widthIsSnapped() {
        Result exact = presenter.compute(defaults());
        Result typed = presenter.compute(new Request("2", "1.04", "-20", "3", "1.5"));
        assertTrue(exact.setup().contains("width a = 1.0000 (10 grid cells of 0.1)"), exact.setup());
        assertTrue(typed.setup().contains("typed 1.0400, rounded to 10 grid cells"), typed.setup());
        assertEquals(exact.finalRight(), typed.finalRight());
        // 1.06 is nearer to 11 cells than to 10, so rounding (not truncation) gives 1.1
        Result up = presenter.compute(new Request("2", "1.06", "-20", "3", "1.5"));
        assertTrue(up.setup().contains("width a = 1.1000 (typed 1.0600, rounded to 11 grid cells"), up.setup());
    }

    @Test
    @DisplayName("the barrier outline peaks at the initial density maximum; time series cover the whole run")
    void seriesAndOutline() {
        Result r = presenter.compute(defaults());
        double peak = 0.0;
        for (double d : r.frames().get(0).density()) {
            peak = Math.max(peak, d);
        }
        assertEquals(6, r.barrierOutline().size());
        assertEquals(peak, r.barrierOutline().get(2).y(), 0.0);
        assertEquals(0.0, r.barrierOutline().get(2).x(), 1e-6);
        assertEquals(1.0, r.barrierOutline().get(3).x(), 1e-12);
        assertTrue(r.maxDensity() >= peak);

        assertEquals(181, r.rightOverTime().size());
        assertEquals(181, r.leftOverTime().size());
        assertEquals(181, r.insideOverTime().size());
        assertEquals(new SeriesPoint(0.0, r.rightOverTime().get(0).y()), r.rightOverTime().get(0));
        assertEquals(36.0, r.rightOverTime().get(180).x(), 1e-9);
        assertEquals(2, r.theoryLine().size());
        assertEquals(0.250530, r.theoryLine().get(0).y(), 1e-6);
        assertEquals(36.0, r.theoryLine().get(1).x(), 1e-12);
        assertEquals(r.theoryLine().get(0).y(), r.theoryLine().get(1).y(), 0.0);
    }

    @Test
    @DisplayName("frame text is pre-formatted")
    void frameText() {
        Result r = presenter.compute(defaults());
        FrameText start = WavePacketPresenter.describe(r.frames().get(0));
        assertEquals("0.00", start.time());
        assertEquals("1.000000", start.total());
        assertEquals("1.000000", start.left());
        FrameText end = WavePacketPresenter.describe(r.frames().get(180));
        assertEquals("36.00", end.time());
        assertEquals(r.finalRight(), end.right());
    }

    @Test
    @DisplayName("the same settings give identical results")
    void deterministic() {
        Result a = presenter.compute(defaults());
        Result b = presenter.compute(defaults());
        assertEquals(a.finalRight(), b.finalRight());
        assertEquals(a.frames().get(90).right(), b.frames().get(90).right(), 0.0);
    }

    @Test
    @DisplayName("settings that would give a meaningless or damaged run are rejected with plain messages")
    void validation() {
        assertTrue(messageOf(new Request("0", "1", "-20", "3", "1.5"), presenter).contains("Barrier height V0"));
        assertTrue(messageOf(new Request("51", "1", "-20", "3", "1.5"), presenter).contains("at most 50"));
        assertTrue(messageOf(new Request("2", "0.05", "-20", "3", "1.5"), presenter).contains("between 0.1 and 10"));
        assertTrue(messageOf(new Request("2", "11", "-20", "3", "1.5"), presenter).contains("between 0.1 and 10"));
        assertTrue(messageOf(new Request("2", "1", "-20", "0.5", "1.5"), presenter).contains("Packet width"));
        assertTrue(messageOf(new Request("2", "1", "-20", "3", "0.4"), presenter).contains("Wave number k0"));
        assertTrue(messageOf(new Request("2", "1", "-20", "1", "2"), presenter).contains("at least 2.5"));
        assertTrue(messageOf(new Request("2", "1", "-10", "3", "1.5"), presenter).contains("entirely to the left"));
        assertTrue(messageOf(new Request("2", "1", "-70", "3", "1.5"), presenter).contains("left wall"));
        assertTrue(messageOf(new Request("2", "1", "-40", "5", "0.5"), presenter).contains("reaches the walls"));
        assertTrue(messageOf(new Request("2", "abc", "-20", "3", "1.5"), presenter).contains("Barrier width a is not a number"));
        assertThrows(InvalidInputException.class, () -> presenter.compute(null));
    }
}
