package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.physics.StandardBasis;
import dev.quantumlab.simulation.SeriesPoint;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InterferencePresenterTest {

    private final InterferencePresenter presenter = new InterferencePresenter();

    private InterferencePresenter.Result compute(String p, StandardBasis basis) {
        return presenter.compute(new InterferencePresenter.Request(p, basis));
    }

    @Test
    @DisplayName("the equal superposition in the X basis is a full fringe")
    void equalSuperpositionInTheXBasis() {
        InterferencePresenter.Result r = compute("0.5", StandardBasis.HADAMARD);

        assertEquals("1.0000", r.height());
        assertEquals("1.0000", r.expectedHeight());
        assertEquals(InterferencePresenter.POINTS, r.superposition().size());
        assertEquals(-180.0, r.superposition().get(0).x(), 1e-9);
        assertEquals(180.0, r.superposition().get(r.superposition().size() - 1).x(), 1e-9);
        assertEquals(2, r.rows().size());
        InterferencePresenter.RowView plus = r.rows().get(0);
        assertEquals("|+⟩", plus.outcome());
        assertEquals("100.00 %", plus.atZero());
        assertEquals("50.00 %", plus.atQuarter());
        assertEquals("0.00 %", plus.atHalf());
        assertEquals("50.00 %", plus.mixture());
        InterferencePresenter.RowView minus = r.rows().get(1);
        assertEquals("0.00 %", minus.atZero());
        assertEquals("100.00 %", minus.atHalf());
    }

    @Test
    @DisplayName("the Z basis has no fringe and agrees with the mixture")
    void zBasisHasNoFringe() {
        InterferencePresenter.Result r = compute("0.3", StandardBasis.COMPUTATIONAL);

        assertEquals("0.0000", r.height());
        assertEquals("0.0000", r.expectedHeight());
        assertTrue(r.verdict().contains("same for every phase"));
        for (SeriesPoint p : r.superposition()) {
            assertEquals(0.3, p.y(), 1e-12);
        }
        for (SeriesPoint p : r.mixture()) {
            assertEquals(0.3, p.y(), 1e-12);
        }
    }

    @Test
    @DisplayName("X basis: the curve follows P(+) = (1 + 2|a||b| cos phi)/2")
    void xBasisClosedForm() {
        double ab = Math.sqrt(0.3) * Math.sqrt(0.7);
        for (SeriesPoint p : compute("0.3", StandardBasis.HADAMARD).superposition()) {
            double phi = Math.toRadians(p.x());
            assertEquals((1 + 2 * ab * Math.cos(phi)) / 2, p.y(), 1e-9);
        }
    }

    @Test
    @DisplayName("Y basis: the curve follows P(+i) = (1 + 2|a||b| sin phi)/2")
    void yBasisClosedForm() {
        double ab = Math.sqrt(0.3) * Math.sqrt(0.7);
        for (SeriesPoint p : compute("0.3", StandardBasis.CIRCULAR).superposition()) {
            double phi = Math.toRadians(p.x());
            assertEquals((1 + 2 * ab * Math.sin(phi)) / 2, p.y(), 1e-9);
        }
    }

    @Test
    @DisplayName("the classical mixture does not depend on the phase")
    void mixtureIsFlat() {
        List<SeriesPoint> x = compute("0.3", StandardBasis.HADAMARD).mixture();
        List<SeriesPoint> y = compute("0.3", StandardBasis.CIRCULAR).mixture();
        for (SeriesPoint p : x) {
            assertEquals(0.5, p.y(), 1e-12);
        }
        for (SeriesPoint p : y) {
            assertEquals(0.5, p.y(), 1e-12);
        }
    }

    @Test
    @DisplayName("the measured fringe height matches 2|a||b| for several chances")
    void heightMatchesTheClosedForm() {
        for (double p0 : new double[] {0.1, 0.3, 0.5, 0.9}) {
            double expected = 2 * Math.sqrt(p0) * Math.sqrt(1 - p0);
            for (StandardBasis b : new StandardBasis[] {StandardBasis.HADAMARD, StandardBasis.CIRCULAR}) {
                InterferencePresenter.Result r = compute(Double.toString(p0), b);
                assertEquals(expected, Double.parseDouble(r.height()), 1e-4, p0 + " " + b);
                assertEquals(expected, Double.parseDouble(r.expectedHeight()), 1e-4, p0 + " " + b);
            }
        }
    }

    @Test
    @DisplayName("a state that is purely one outcome shows no interference")
    void pureStateHasNoFringe() {
        for (String p : new String[] {"0", "1"}) {
            InterferencePresenter.Result r = compute(p, StandardBasis.HADAMARD);
            assertEquals("0.0000", r.height());
            assertTrue(r.verdict().contains("same for every phase"));
        }
    }

    @Test
    @DisplayName("a fringe is described by its largest and smallest probability")
    void verdictNamesTheSwing() {
        InterferencePresenter.Result r = compute("0.5", StandardBasis.HADAMARD);
        assertTrue(r.verdict().contains("100.00 %"));
        assertTrue(r.verdict().contains("0.00 %"));
        assertTrue(r.verdict().contains("50.00 %"));
    }

    @Test
    @DisplayName("invalid input is rejected with a plain message")
    void invalidInput() {
        assertThrows(InvalidInputException.class, () -> compute("abc", StandardBasis.HADAMARD));
        InvalidInputException high =
            assertThrows(InvalidInputException.class, () -> compute("1.5", StandardBasis.HADAMARD));
        assertTrue(high.getMessage().contains("between 0 and 1"));
        assertThrows(InvalidInputException.class, () -> compute("-0.1", StandardBasis.HADAMARD));
        assertThrows(InvalidInputException.class, () -> compute("0.5", null));
        assertThrows(InvalidInputException.class, () -> presenter.compute(null));
    }

    @Test
    @DisplayName("the same input gives the same result")
    void deterministic() {
        assertEquals(compute("0.37", StandardBasis.CIRCULAR), compute("0.37", StandardBasis.CIRCULAR));
    }
}
