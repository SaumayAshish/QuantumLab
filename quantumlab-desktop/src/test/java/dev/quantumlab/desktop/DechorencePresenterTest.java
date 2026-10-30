package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.desktop.CompositePresenter.BasisChoice;
import dev.quantumlab.desktop.DecoherencePresenter.Request;
import dev.quantumlab.desktop.DecoherencePresenter.Result;
import dev.quantumlab.simulation.SeriesPoint;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DecoherencePresenterTest {

    private static final BasisChoice Z = BasisChoice.COMPUTATIONAL;
    private static final BasisChoice X = BasisChoice.HADAMARD;
    private static final BasisChoice Y = BasisChoice.CIRCULAR;
    private static final String HALF_PI = "1.5707963267948966";

    private final DecoherencePresenter presenter = new DecoherencePresenter();

    private Result run(String p0, String phase, String rate, String time, BasisChoice basis) {
        return presenter.compute(new Request(p0, phase, rate, time, basis));
    }

    /** 0.6|0⟩ + 0.8i|1⟩ dephased at rate 0.5 for time 2, as in the physics tests. */
    private Result reference(BasisChoice basis) {
        return run("0.36", HALF_PI, "0.5", "2", basis);
    }

    private static double number(String text) {
        return Double.parseDouble(text.replace('−', '-'));
    }

    @Test
    @DisplayName("Y-basis probabilities at t = 0, at the chosen time and in the limit match independent values")
    void yBasisRows() {
        Result r = reference(Y);
        assertEquals(2, r.rows().size());
        assertTrue(r.rows().get(0).outcome().startsWith("nucleus "));
        assertEquals("0.980000", r.rows().get(0).initial());
        assertEquals("0.676582", r.rows().get(0).now());
        assertEquals("0.500000", r.rows().get(0).limit());
        assertEquals("0.020000", r.rows().get(1).initial());
        assertEquals("0.323418", r.rows().get(1).now());
        assertEquals("0.500000", r.rows().get(1).limit());
    }

    @Test
    @DisplayName("X-basis probabilities follow the real part of the coherence, here 0.5 throughout")
    void xBasisRows() {
        Result r = run("0.5", "0", "1", "1", X);
        assertEquals("1.000000", r.rows().get(0).initial());
        assertEquals("0.683940", r.rows().get(0).now());
        assertEquals("0.500000", r.rows().get(0).limit());
        assertEquals("0", r.rows().get(1).initial());
        assertEquals("0.316060", r.rows().get(1).now());
        Result flat = reference(X);
        assertEquals("0.500000", flat.rows().get(0).initial());
        assertEquals("0.500000", flat.rows().get(0).now());
    }

    @Test
    @DisplayName("Z-basis rows never change, and the sentence says so with the numbers")
    void zBasisUnchanged() {
        Result r = reference(Z);
        for (DecoherencePresenter.RowView row : r.rows()) {
            assertEquals(row.initial(), row.now());
            assertEquals(row.initial(), row.limit());
        }
        assertEquals("0.360000", r.rows().get(0).initial());
        assertEquals("0.640000", r.rows().get(1).initial());
        assertTrue(r.zChances().contains("0.360000 / 0.640000 at t = 0, and 0.360000 / 0.640000 at the chosen time"),
            r.zChances());
    }

    @Test
    @DisplayName("visibility, purity, entropy, Bloch vector and factor match independent values")
    void derivedQuantities() {
        Result r = reference(Y);
        assertEquals("0.367879", r.factor());
        assertEquals("0.960000", r.visibilityInitial());
        assertEquals("0.353164", r.visibilityNow());
        assertEquals("1.000000", r.purityInitial());
        assertEquals("0.601562", r.purityNow());
        assertEquals("0.000000 bits", r.entropyInitial());
        assertEquals("0.848063 bits", r.entropyNow());
        assertEquals("(0.0000, 0.3532, −0.2800)", r.bloch());
        assertEquals("Coherence time 1/γ = 2.0000.", r.coherenceTime());
    }

    @Test
    @DisplayName("a second state, with real coherence, also matches independent values")
    void secondState() {
        Result r = run("0.8", "0", "0.3", "5", X);
        assertEquals("0.900000", r.rows().get(0).initial());
        assertEquals("0.589252", r.rows().get(0).now());
        assertEquals("0.178504", r.visibilityNow());
        assertEquals("0.695932", r.purityNow());
        assertEquals("0.695164 bits", r.entropyNow());
        assertEquals("0.223130", r.factor());
    }

    @Test
    @DisplayName("the setup line repeats what was typed")
    void setupLine() {
        Result r = reference(Y);
        assertTrue(r.setup().contains("|α|² = 0.3600"), r.setup());
        assertTrue(r.setup().contains("|β|² = 0.6400"), r.setup());
        assertTrue(r.setup().contains("γ = 0.5000"), r.setup());
        assertTrue(r.setup().contains("t = 2.0000"), r.setup());
        assertTrue(r.setup().contains("e^(−γt) = 0.367879"), r.setup());
        assertTrue(r.basis().contains("Y") || r.basis().toLowerCase().contains("circular"), r.basis());
    }

    @Test
    @DisplayName("time zero or a zero rate leaves the state as it started")
    void nothingHappens() {
        Result atZero = run("0.5", "0", "2", "0", X);
        assertEquals("1.000000", atZero.factor());
        assertEquals(atZero.visibilityInitial(), atZero.visibilityNow());
        assertEquals(atZero.rows().get(0).initial(), atZero.rows().get(0).now());
        Result noRate = run("0.5", "0", "0", "100", X);
        assertEquals("1.000000", noRate.factor());
        assertEquals("With a rate of 0 the coherence never decays.", noRate.coherenceTime());
        assertEquals(noRate.purityInitial(), noRate.purityNow());
    }

    @Test
    @DisplayName("a long time gives the classical mixture: no visibility and the purity of the populations")
    void longTime() {
        Result r = run("0.36", "0", "10", "100", X);
        assertEquals("0", r.factor());
        assertEquals("0", r.visibilityNow());
        assertEquals("0.500000", r.rows().get(0).now());
        assertEquals("0.500000", r.rows().get(1).now());
        assertEquals("0.539200", r.purityNow()); // 0.36² + 0.64²
    }

    @Test
    @DisplayName("three tiers of verdict: nearly all left, partly gone, nearly all gone")
    void verdictTiers() {
        assertTrue(run("0.5", "0", "0.001", "1", X).verdict().contains("Almost all of the coherence is still there"));
        String partly = run("0.5", "0", "0.5", "2", X).verdict();
        assertTrue(partly.contains("Part of the coherence is gone"), partly);
        assertTrue(partly.contains("36.8% of it is left"), partly);
        String gone = run("0.5", "0", "10", "1", X).verdict();
        assertTrue(gone.contains("Almost all of the coherence is gone"), gone);
        assertTrue(gone.contains("not the same as one outcome having happened"), gone);
    }

    @Test
    @DisplayName("the tier boundaries are at 99% and 1% of the coherence")
    void verdictBoundaries() {
        // e^{-γt} = 0.99 at γt = -ln 0.99 = 0.01005034; slightly smaller is "still there", slightly larger is not.
        assertTrue(run("0.5", "0", "0.0100", "1", X).verdict().contains("still there"));
        assertTrue(run("0.5", "0", "0.0101", "1", X).verdict().contains("Part of the coherence"));
        // e^{-γt} = 0.01 at γt = 4.60517; slightly smaller is "partly", slightly larger is "gone".
        assertTrue(run("0.5", "0", "4.60", "1", X).verdict().contains("Part of the coherence"));
        assertTrue(run("0.5", "0", "4.61", "1", X).verdict().contains("Almost all of the coherence is gone"));
    }

    @Test
    @DisplayName("a start in |0⟩ or |1⟩ has no coherence to lose, and the screen says so")
    void noCoherenceToLose() {
        for (String p0 : new String[] {"0", "1"}) {
            Result r = run(p0, "0", "1", "1", X);
            assertTrue(r.verdict().contains("no coherence"), r.verdict());
            assertEquals("0", r.visibilityInitial());
            assertEquals("0.500000", r.rows().get(0).initial());
            assertEquals("1.000000", r.purityNow());
        }
    }

    @Test
    @DisplayName("the time axis covers five coherence times, or the chosen time if later")
    void timeSpan() {
        assertEquals(10.0, run("0.5", "0", "0.5", "1", X).timeSpan(), 1e-12);
        assertEquals(40.0, run("0.5", "0", "1", "40", X).timeSpan(), 1e-12);
        assertEquals(1.0, run("0.5", "0", "0", "0.5", X).timeSpan(), 1e-12);
        assertEquals(7.0, run("0.5", "0", "0", "7", X).timeSpan(), 1e-12);
    }

    @Test
    @DisplayName("the time curves run from 0 to the span, start at the initial values and only fall or rise")
    void timeCurves() {
        Result r = reference(Y);
        List<SeriesPoint> v = r.visibilityCurve();
        List<SeriesPoint> p = r.purityCurve();
        List<SeriesPoint> h = r.entropyCurve();
        assertEquals(DecoherencePresenter.TIME_POINTS, v.size());
        assertEquals(DecoherencePresenter.TIME_POINTS, p.size());
        assertEquals(DecoherencePresenter.TIME_POINTS, h.size());
        assertEquals(0.0, v.get(0).x(), 0.0);
        assertEquals(r.timeSpan(), v.get(v.size() - 1).x(), 1e-12);
        assertEquals(0.96, v.get(0).y(), 1e-12);
        assertEquals(1.0, p.get(0).y(), 1e-12);
        assertEquals(0.0, h.get(0).y(), 1e-6);
        for (int k = 1; k < v.size(); k++) {
            assertTrue(v.get(k).y() <= v.get(k - 1).y() + 1e-12);
            assertTrue(p.get(k).y() <= p.get(k - 1).y() + 1e-12);
            assertTrue(h.get(k).y() >= h.get(k - 1).y() - 1e-12);
        }
        // At t = 2 (index 20 of 100 on a span of 10) the curves hold the values shown in the table.
        assertEquals(2.0, v.get(20).x(), 1e-12);
        assertEquals(number(r.visibilityNow()), v.get(20).y(), 1e-6);
        assertEquals(number(r.purityNow()), p.get(20).y(), 1e-6);
    }

    @Test
    @DisplayName("the fringe has its maximum minus minimum equal to the visibility, at t = 0 and now")
    void fringes() {
        Result r = reference(Y);
        assertEquals(DecoherencePresenter.FRINGE_POINTS, r.fringeInitial().size());
        assertEquals(DecoherencePresenter.FRINGE_POINTS, r.fringeNow().size());
        assertEquals(0.0, r.fringeInitial().get(0).x(), 0.0);
        assertEquals(360.0, r.fringeInitial().get(180).x(), 0.0);
        assertEquals(number(r.visibilityInitial()), height(r.fringeInitial()), 1e-6);
        assertEquals(number(r.visibilityNow()), height(r.fringeNow()), 1e-6);
        // The curve is sampled every 2°, so check the 40° point against the closed form.
        double expected = 0.5 + 0.48 * Math.exp(-1.0) * Math.sin(Math.toRadians(40.0));
        assertEquals(expected, r.fringeNow().get(20).y(), 1e-12);
    }

    private static double height(List<SeriesPoint> curve) {
        double max = 0.0;
        double min = 1.0;
        for (SeriesPoint p : curve) {
            max = Math.max(max, p.y());
            min = Math.min(min, p.y());
        }
        return max - min;
    }

    @Test
    @DisplayName("invalid input is rejected with a message that names the field")
    void validation() {
        assertMessage(run0("1.5", "0", "1", "1"), "Probability of \"not decayed\" must be between 0 and 1.");
        assertMessage(run0("-0.1", "0", "1", "1"), "Probability of \"not decayed\" must be between 0 and 1.");
        assertMessage(run0("0.5", "2000", "1", "1"), "Relative phase must be between −1000 and 1000 radians.");
        assertMessage(run0("0.5", "0", "-1", "1"), "Dephasing rate must be between 0 and 1000.");
        assertMessage(run0("0.5", "0", "1001", "1"), "Dephasing rate must be between 0 and 1000.");
        assertMessage(run0("0.5", "0", "1", "-1"), "Time must be between 0 and 1000.");
        assertMessage(run0("0.5", "0", "1", "1001"), "Time must be between 0 and 1000.");
        assertMessage(run0("abc", "0", "1", "1"), "Probability");
        assertMessage(run0("0.5", "0", "", "1"), "Dephasing rate");
        assertMessage(run0("0.5", "0", "1", ""), "Time");
        assertThrows(InvalidInputException.class, () -> presenter.compute(null));
        assertThrows(InvalidInputException.class, () -> presenter.compute(new Request("0.5", "0", "1", "1", null)));
    }

    private Runnable run0(String p0, String phase, String rate, String time) {
        return () -> run(p0, phase, rate, time, X);
    }

    private static void assertMessage(Runnable action, String expectedFragment) {
        InvalidInputException e = assertThrows(InvalidInputException.class, action::run);
        assertTrue(e.getMessage().contains(expectedFragment), e.getMessage());
    }

    @Test
    @DisplayName("the limits themselves are accepted")
    void limitsAccepted() {
        run("0", "-1000", "0", "0", Z);
        run("1", "1000", "1000", "1000", Z);
        Result r = run("0.5", "0", "1000", "1000", X);
        assertEquals("0", r.factor());
        assertEquals(1000.0, r.timeSpan(), 1e-12);
    }

    @Test
    @DisplayName("results are repeatable")
    void deterministic() {
        assertEquals(reference(Y), reference(Y));
    }

    @Test
    @DisplayName("a small but real coherence is not reported as none")
    void smallCoherenceIsStillCoherence() {
        // p0 = 0.999 gives |ρ01| = sqrt(0.999 × 0.001) = 0.0316, a visibility of 0.063214.
        Result r = run("0.999", "0", "0.5", "2", X);
        assertEquals("0.063214", r.visibilityInitial());
        assertTrue(!r.verdict().contains("no coherence"), r.verdict());
        assertTrue(r.verdict().contains("Part of the coherence is gone"), r.verdict());
    }

    @Test
    @DisplayName("outcome probabilities below rounding noise are shown as 0, never as a residue")
    void noiseIsShownAsZero() {
        assertEquals("0", DecoherencePresenter.probability(-5.551e-17));
        assertEquals("0", DecoherencePresenter.probability(1.0e-13));
        assertEquals("0", DecoherencePresenter.probability(0.0));
        assertEquals("1.000000", DecoherencePresenter.probability(1.0));
        assertEquals("2.000e-11", DecoherencePresenter.probability(2.0e-11));
    }

    @Test
    @DisplayName("a really tiny decay factor is shown as it is, not rounded to 0")
    void tinyFactorIsNotZero() {
        // e^(−100) = 3.7200759760208555e-44
        Result r = run("0.5", "0", "10", "10", X);
        assertEquals("3.720e-44", r.factor());
        assertEquals("3.720e-44", r.visibilityNow());
        assertTrue(r.setup().contains("e^(−γt) = 3.720e-44"), r.setup());
    }
}
