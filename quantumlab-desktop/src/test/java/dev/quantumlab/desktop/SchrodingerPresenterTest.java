package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.desktop.SchrodingerPresenter.PotentialKind;
import dev.quantumlab.desktop.SchrodingerPresenter.Request;
import dev.quantumlab.desktop.SchrodingerPresenter.SolutionView;
import dev.quantumlab.desktop.SchrodingerPresenter.WaveView;
import dev.quantumlab.simulation.SeriesPoint;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SchrodingerPresenterTest {

    private static Request well(String xMin, String xMax, String points, String levels) {
        return new Request(PotentialKind.INFINITE_WELL, xMin, xMax, points, "1", levels);
    }

    private static Request harmonic(String omega, String xMin, String xMax, String points, String levels) {
        return new Request(PotentialKind.HARMONIC, xMin, xMax, points, omega, levels);
    }

    private static SolutionView solved(SchrodingerPresenter p, Request r) {
        return p.solve(r);
    }

    @Test
    @DisplayName("the well table shows numerical levels beside the exact n²π²/2L², from an independent calculation")
    void wellTable() {
        // Numerical values: numpy.linalg.eigvalsh on the same 999-point matrix, outside this code base.
        SolutionView v = solved(new SchrodingerPresenter(), well("0", "1", "999", "6"));
        assertEquals(6, v.levels().size());
        String[] numerical = {"4.934798", "19.739144", "44.412891", "78.955796", "123.367518", "177.647619"};
        String[] reference = {"4.934802", "19.739209", "44.413220", "78.956835", "123.370055", "177.652879"};
        String[] error = {"−0.000004", "−0.000065", "−0.000329", "−0.001039", "−0.002537", "−0.005260"};
        for (int k = 0; k < 6; k++) {
            assertEquals("n = " + (k + 1), v.levels().get(k).label());
            assertEquals(numerical[k], v.levels().get(k).numerical());
            assertEquals(reference[k], v.levels().get(k).reference());
            assertEquals(error[k], v.levels().get(k).error());
        }
    }

    @Test
    @DisplayName("the oscillator table is labelled from n = 0 and compares with ω(n + ½)")
    void harmonicTable() {
        SolutionView v = solved(new SchrodingerPresenter(), harmonic("1", "-10", "10", "2000", "6"));
        String[] numerical = {"0.499997", "1.499984", "2.499959", "3.499922", "4.499872", "5.499810"};
        String[] reference = {"0.500000", "1.500000", "2.500000", "3.500000", "4.500000", "5.500000"};
        String[] error = {"−0.000003", "−0.000016", "−0.000041", "−0.000078", "−0.000128", "−0.000190"};
        for (int k = 0; k < 6; k++) {
            assertEquals("n = " + k, v.levels().get(k).label());
            assertEquals(numerical[k], v.levels().get(k).numerical());
            assertEquals(reference[k], v.levels().get(k).reference());
            assertEquals(error[k], v.levels().get(k).error());
        }
    }

    @Test
    @DisplayName("the oscillator frequency scales the levels: ω = 2 gives 1, 3, 5, 7")
    void frequencyScalesLevels() {
        SolutionView v = solved(new SchrodingerPresenter(), harmonic("2", "-8", "8", "1000", "4"));
        assertEquals("1.000000", v.levels().get(0).reference());
        assertEquals("3.000000", v.levels().get(1).reference());
        assertEquals("5.000000", v.levels().get(2).reference());
        assertEquals("7.000000", v.levels().get(3).reference());
        assertEquals("0.999968", v.levels().get(0).numerical());
        assertEquals("2.999840", v.levels().get(1).numerical());
    }

    @Test
    @DisplayName("the setup view states the potential, domain, grid size and spacing")
    void setupView() {
        SchrodingerPresenter.SetupView s =
            solved(new SchrodingerPresenter(), well("0", "1", "999", "3")).setup();
        assertTrue(s.potential().contains("hard walls"));
        assertEquals("[0.0000, 1.0000]", s.domain());
        assertEquals("999", s.gridPoints());
        assertEquals("0.001000", s.spacing());
        assertFalse(s.note().isEmpty());
    }

    @Test
    @DisplayName("the relative error is given in percent, and it grows with the level for the well")
    void relativeError() {
        SolutionView v = solved(new SchrodingerPresenter(), well("0", "1", "199", "5"));
        double previous = 0.0;
        for (SchrodingerPresenter.LevelRow row : v.levels()) {
            assertTrue(row.relativeError().endsWith(" %"));
            double magnitude = Math.abs(Double.parseDouble(
                row.relativeError().replace(" %", "").replace('−', '-')));
            assertTrue(magnitude > previous, row.label());
            previous = magnitude;
        }
    }

    @Test
    @DisplayName("invalid settings are rejected with messages that say what is wrong")
    void invalidInput() {
        SchrodingerPresenter p = new SchrodingerPresenter();
        assertThrows(InvalidInputException.class, () -> p.solve(null));
        assertThrows(InvalidInputException.class, () -> p.solve(new Request(null, "0", "1", "100", "1", "3")));
        assertTrue(assertThrows(InvalidInputException.class, () -> p.solve(well("abc", "1", "100", "3")))
            .getMessage().contains("Left wall"));
        assertTrue(assertThrows(InvalidInputException.class, () -> p.solve(well("0", "", "100", "3")))
            .getMessage().contains("Right wall"));
        assertTrue(assertThrows(InvalidInputException.class, () -> p.solve(well("1", "1", "100", "3")))
            .getMessage().contains("right of the left"));
        assertTrue(assertThrows(InvalidInputException.class, () -> p.solve(well("2", "1", "100", "3")))
            .getMessage().contains("right of the left"));
        assertTrue(assertThrows(InvalidInputException.class, () -> p.solve(well("0", "1e9", "100", "3")))
            .getMessage().contains("within"));
        assertTrue(assertThrows(InvalidInputException.class, () -> p.solve(well("0", "1", "5", "3")))
            .getMessage().contains("Grid points"));
        assertTrue(assertThrows(InvalidInputException.class, () -> p.solve(well("0", "1", "20001", "3")))
            .getMessage().contains("Grid points"));
        assertTrue(assertThrows(InvalidInputException.class, () -> p.solve(well("0", "1", "100.5", "3")))
            .getMessage().contains("whole number"));
        assertTrue(assertThrows(InvalidInputException.class, () -> p.solve(well("0", "1", "100", "0")))
            .getMessage().contains("Number of levels"));
        assertTrue(assertThrows(InvalidInputException.class, () -> p.solve(well("0", "1", "100", "21")))
            .getMessage().contains("Number of levels"));
        assertTrue(assertThrows(InvalidInputException.class, () -> p.solve(harmonic("0", "-5", "5", "100", "3")))
            .getMessage().contains("ω"));
        assertTrue(assertThrows(InvalidInputException.class, () -> p.solve(harmonic("-1", "-5", "5", "100", "3")))
            .getMessage().contains("ω"));
        assertThrows(InvalidInputException.class, () -> p.solve(harmonic("NaN", "-5", "5", "100", "3")));
        assertFalse(p.hasSolution());
    }

    @Test
    @DisplayName("the oscillator frequency is ignored, and not validated, for the well")
    void omegaIgnoredForWell() {
        SchrodingerPresenter p = new SchrodingerPresenter();
        Request r = new Request(PotentialKind.INFINITE_WELL, "0", "1", "99", "not a number", "2");
        assertEquals(2, p.solve(r).levels().size());
    }

    @Test
    @DisplayName("a failed solve leaves the previous solution untouched")
    void failedSolveKeepsPrevious() {
        SchrodingerPresenter p = new SchrodingerPresenter();
        p.solve(well("0", "1", "199", "4"));
        assertThrows(InvalidInputException.class, () -> p.solve(well("x", "1", "199", "4")));
        assertEquals("n = 1", p.wave(0).label());
        assertThrows(InvalidInputException.class, () -> p.wave(4));
    }

    @Test
    @DisplayName("asking for a wavefunction before solving says so")
    void requiresSolution() {
        assertThrows(IllegalStateException.class, () -> new SchrodingerPresenter().wave(0));
    }

    @Test
    @DisplayName("a level index outside the solved levels is rejected")
    void waveIndexRange() {
        SchrodingerPresenter p = new SchrodingerPresenter();
        p.solve(well("0", "1", "99", "3"));
        assertThrows(InvalidInputException.class, () -> p.wave(-1));
        assertThrows(InvalidInputException.class, () -> p.wave(3));
        assertEquals("n = 3", p.wave(2).label());
    }

    @Test
    @DisplayName("a well wavefunction is plotted from wall to wall, normalised, and sits on the exact curve")
    void wellWave() {
        SchrodingerPresenter p = new SchrodingerPresenter();
        p.solve(well("0", "2", "199", "4"));
        WaveView w = p.wave(1);
        assertEquals("n = 2", w.label());
        assertEquals(1, w.nodes());
        assertEquals("1.000000", w.normalisation());
        assertEquals(0.0, w.amplitude().get(0).x(), 0.0);
        assertEquals(2.0, w.amplitude().get(w.amplitude().size() - 1).x(), 0.0);
        assertEquals(0.0, w.amplitude().get(0).y(), 0.0);
        assertEquals(w.amplitude().size(), w.exact().size());
        for (int i = 0; i < w.amplitude().size(); i++) {
            assertEquals(w.exact().get(i).y(), w.amplitude().get(i).y(), 1e-9);
            assertEquals(w.exact().get(i).x(), w.amplitude().get(i).x(), 1e-12);
        }
        double density = 0.0;
        for (int i = 0; i < w.amplitude().size(); i++) {
            assertEquals(
                w.amplitude().get(i).y() * w.amplitude().get(i).y(), w.density().get(i).y(), 1e-12);
            density += w.density().get(i).y();
        }
        assertTrue(density > 0.0);
    }

    @Test
    @DisplayName("an oscillator wavefunction has n nodes, a potential curve, and no exact overlay")
    void harmonicWave() {
        SchrodingerPresenter p = new SchrodingerPresenter();
        p.solve(harmonic("1", "-10", "10", "2000", "6"));
        for (int k = 0; k < 6; k++) {
            WaveView w = p.wave(k);
            assertEquals("n = " + k, w.label());
            assertEquals(k, w.nodes());
            assertEquals("1.000000", w.normalisation());
            assertTrue(w.exact().isEmpty());
            assertFalse(w.potential().isEmpty());
        }
        WaveView ground = p.wave(0);
        assertEquals("0.499997", ground.energy());
        for (SeriesPoint pt : ground.potential()) {
            assertEquals(0.5 * pt.x() * pt.x(), pt.y(), 1e-9);
        }
    }

    @Test
    @DisplayName("long grids are thinned for plotting but keep both walls and the last point")
    void plotThinning() {
        SchrodingerPresenter p = new SchrodingerPresenter();
        p.solve(well("0", "1", "5000", "2"));
        WaveView w = p.wave(0);
        assertTrue(w.amplitude().size() <= SchrodingerPresenter.MAX_PLOT_POINTS + 3);
        assertTrue(w.amplitude().size() > 100);
        assertEquals(0.0, w.amplitude().get(0).x(), 0.0);
        assertEquals(1.0, w.amplitude().get(w.amplitude().size() - 1).x(), 0.0);
        double lastInterior = w.amplitude().get(w.amplitude().size() - 2).x();
        assertEquals(5000.0 / 5001.0, lastInterior, 1e-12);
    }

    @Test
    @DisplayName("a box that is too small for the requested oscillator levels says so")
    void smallBoxWarning() {
        SchrodingerPresenter p = new SchrodingerPresenter();
        String roomy = p.solve(harmonic("1", "-10", "10", "1000", "6")).setup().note();
        assertFalse(roomy.contains("beyond the nearer wall"));
        String cramped = p.solve(harmonic("1", "-3", "3", "1000", "10")).setup().note();
        assertTrue(cramped.contains("beyond the nearer wall"));
        assertTrue(cramped.contains("From n = "));
        String offCentre = p.solve(harmonic("1", "1", "9", "1000", "3")).setup().note();
        assertTrue(offCentre.contains("does not contain the potential minimum"));
    }

    @Test
    @DisplayName("solving the same settings twice gives identical views")
    void deterministic() {
        Request r = harmonic("1.5", "-8", "8", "600", "5");
        SchrodingerPresenter a = new SchrodingerPresenter();
        SchrodingerPresenter b = new SchrodingerPresenter();
        assertEquals(a.solve(r), b.solve(r));
        assertEquals(a.wave(3), b.wave(3));
        List<SeriesPoint> first = a.wave(2).amplitude();
        assertEquals(first, a.wave(2).amplitude());
    }
}
