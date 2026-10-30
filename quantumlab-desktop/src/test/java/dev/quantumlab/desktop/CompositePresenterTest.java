package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.desktop.CompositePresenter.BasisChoice;
import dev.quantumlab.desktop.CompositePresenter.Request;
import dev.quantumlab.desktop.CompositePresenter.Result;
import dev.quantumlab.desktop.CompositePresenter.RowView;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompositePresenterTest {

    private static final BasisChoice Z = BasisChoice.COMPUTATIONAL;
    private static final BasisChoice X = BasisChoice.HADAMARD;
    private static final BasisChoice Y = BasisChoice.CIRCULAR;

    private final CompositePresenter presenter = new CompositePresenter();

    private Result run(String p0, String phase, BasisChoice nucleus, BasisChoice detector) {
        return presenter.compute(new Request(p0, phase, nucleus, detector));
    }

    private static double number(String text) {
        return Double.parseDouble(text.replace('−', '-'));
    }

    private static double[] column(List<RowView> rows, boolean entangled) {
        double[] v = new double[4];
        for (int i = 0; i < 4; i++) {
            v[i] = number(entangled ? rows.get(i).entangled() : rows.get(i).mixture());
        }
        return v;
    }

    private static void assertRow(RowView row, String outcome, String entangled, String mixture, String difference) {
        assertEquals(outcome, row.outcome());
        assertEquals(entangled, row.entangled());
        assertEquals(mixture, row.mixture());
        assertEquals(difference, row.difference());
    }

    private static String messageOf(Request r, CompositePresenter p) {
        return assertThrows(InvalidInputException.class, () -> p.compute(r)).getMessage();
    }

    @Test
    @DisplayName("an equal superposition measured in Z and Z: the entangled state and the mixture are indistinguishable")
    void sameInComputationalBasis() {
        Result r = run("0.5", "0", Z, Z);
        assertEquals(4, r.rows().size());
        assertRow(r.rows().get(0), "nucleus |0⟩, detector |0⟩", "0.500000", "0.500000", "0.000000");
        assertRow(r.rows().get(1), "nucleus |0⟩, detector |1⟩", "0", "0", "0.000000");
        assertRow(r.rows().get(2), "nucleus |1⟩, detector |0⟩", "0", "0", "0.000000");
        assertRow(r.rows().get(3), "nucleus |1⟩, detector |1⟩", "0.500000", "0.500000", "0.000000");
        assertEquals("1.000000", r.agreeEntangled());
        assertEquals("1.000000", r.agreeMixture());
        assertTrue(r.verdictBases().contains("same statistics"));
        assertTrue(r.bases().contains("Computational (Z)"));
    }

    @Test
    @DisplayName("measured in X and X the entangled state agrees every time and the mixture is a coin toss")
    void differsInHadamardBasis() {
        Result r = run("0.5", "0", X, X);
        assertRow(r.rows().get(0), "nucleus |+⟩, detector |+⟩", "0.500000", "0.250000", "0.250000");
        assertRow(r.rows().get(1), "nucleus |+⟩, detector |−⟩", "0", "0.250000", "−0.250000");
        assertRow(r.rows().get(2), "nucleus |−⟩, detector |+⟩", "0", "0.250000", "−0.250000");
        assertRow(r.rows().get(3), "nucleus |−⟩, detector |−⟩", "0.500000", "0.250000", "0.250000");
        assertEquals("1.000000", r.agreeEntangled());
        assertEquals("0.500000", r.agreeMixture());
        assertTrue(r.verdictBases().contains("different joint statistics"));
    }

    @Test
    @DisplayName("measured in Y and Y the entangled state disagrees every time")
    void differsInCircularBasis() {
        Result r = run("0.5", "0", Y, Y);
        assertEquals("0", r.agreeEntangled());
        assertEquals("0.500000", r.agreeMixture());
        assertEquals("0", r.rows().get(0).entangled());
        assertEquals("0.500000", r.rows().get(1).entangled());
        assertEquals("0.500000", r.rows().get(2).entangled());
    }

    @Test
    @DisplayName("the relative phase decides which bases reveal the correlation: at phi = pi/2, Y and Y no longer do but X and Y do")
    void phaseChoosesTheRevealingBases() {
        double halfPi = Math.PI / 2.0;
        Result yy = run("0.5", Double.toString(halfPi), Y, Y);
        assertTrue(yy.verdictBases().contains("same statistics"));
        assertEquals("0.500000", yy.agreeEntangled());
        Result xy = run("0.5", Double.toString(halfPi), X, Y);
        assertTrue(xy.verdictBases().contains("different joint statistics"));
        assertEquals("1.000000", xy.agreeEntangled());
        assertEquals("0.500000", xy.agreeMixture());
    }

    @Test
    @DisplayName("a nucleus in X and a detector in Z cannot tell the two apart, whatever the probability")
    void mixedBasesCannotTellApart() {
        Result r = run("0.8", "0", X, Z);
        assertTrue(r.verdictBases().contains("same statistics"));
        assertEquals(0.4, number(r.rows().get(0).entangled()), 1e-6);
        assertEquals(0.1, number(r.rows().get(1).entangled()), 1e-6);
        assertEquals(0.4, number(r.rows().get(2).entangled()), 1e-6);
        assertEquals(0.1, number(r.rows().get(3).entangled()), 1e-6);
    }

    @Test
    @DisplayName("each qubit on its own looks identical for the entangled state and the mixture")
    void marginalsMatch() {
        Result r = run("0.8", "1.0", X, X);
        assertTrue(r.alone().contains("nucleus |+⟩ 0.500000 (entangled) and 0.500000 (mixture)"), r.alone());
        assertTrue(r.alone().contains("detector |+⟩ 0.500000 (entangled) and 0.500000 (mixture)"), r.alone());
        Result z = run("0.3", "0", Z, Z);
        assertTrue(z.alone().contains("nucleus |0⟩ 0.300000 (entangled) and 0.300000 (mixture)"), z.alone());
    }

    @Test
    @DisplayName("unequal probabilities and a phase match the independent reference (X, X, p0 = 0.8, phi = 1)")
    void referenceValues() {
        Result r = run("0.8", "1.0", X, X);
        double[] e = column(r.rows(), true);
        assertEquals(0.358060, e[0], 1e-6);
        assertEquals(0.141940, e[1], 1e-6);
        assertEquals(0.141940, e[2], 1e-6);
        assertEquals(0.358060, e[3], 1e-6);
        assertEquals(0.716121, number(r.agreeEntangled()), 1e-6);
        for (double m : column(r.rows(), false)) {
            assertEquals(0.25, m, 1e-12);
        }
        assertEquals(0.108060, number(r.rows().get(0).difference()), 1e-6);
        Result zero = run("0.8", "0", X, X);
        assertEquals(0.9, number(zero.agreeEntangled()), 1e-9);
    }

    @Test
    @DisplayName("joint probabilities sum to 1 for both states in every pair of bases")
    void sumsToOne() {
        for (BasisChoice a : BasisChoice.values()) {
            for (BasisChoice b : BasisChoice.values()) {
                Result r = run("0.37", "0.9", a, b);
                double se = 0;
                double sm = 0;
                for (double v : column(r.rows(), true)) {
                    se += v;
                }
                for (double v : column(r.rows(), false)) {
                    sm += v;
                }
                assertEquals(1.0, se, 4e-6);
                assertEquals(1.0, sm, 4e-6);
            }
        }
    }

    @Test
    @DisplayName("entanglement of an equal superposition: concurrence 1, entropy 1 bit, purity 1/2")
    void maximalEntanglement() {
        Result r = run("0.5", "0", Z, Z);
        assertEquals("1.000000", r.concurrence());
        assertEquals("1.000000 bits", r.entropy());
        assertEquals("0.500000", r.purity());
        assertTrue(r.entanglementNote().startsWith("Entangled"));
    }

    @Test
    @DisplayName("entanglement of p0 = 0.8 matches the reference: C = 0.8, 0.721928 bits, purity 0.68")
    void partialEntanglement() {
        Result r = run("0.8", "0", Z, Z);
        assertEquals("0.800000", r.concurrence());
        assertEquals("0.721928 bits", r.entropy());
        assertEquals("0.680000", r.purity());
    }

    @Test
    @DisplayName("a nucleus that is certainly not decayed (or certainly decayed) is not entangled and cannot violate CHSH")
    void productStates() {
        for (String p0 : new String[] {"1", "0"}) {
            Result r = run(p0, "0.4", X, X);
            assertEquals("0.000000", r.concurrence());
            assertEquals("0.000000 bits", r.entropy());
            assertEquals("1.000000", r.purity());
            assertTrue(r.entanglementNote().startsWith("Not entangled"));
            assertEquals("2.0000", r.chshEntangled());
            assertEquals("2.0000", r.chshMaximum());
            assertTrue(r.chshVerdict().startsWith("S does not exceed 2"));
            // With no uncertainty the entangled and mixed descriptions are the same state.
            for (RowView row : r.rows()) {
                assertEquals("0.000000", row.difference());
            }
        }
    }

    @Test
    @DisplayName("CHSH for an equal superposition: 2.8284 for the entangled state, 1.4142 for the mixture at the same settings, 2.0000 at best")
    void chshBell() {
        Result r = run("0.5", "0", Z, Z);
        assertEquals("2.8284", r.chshEntangled());
        assertEquals("1.4142", r.chshMixtureSame());
        assertEquals("2.0000", r.chshMixtureBest());
        assertEquals("2.8284", r.chshMaximum());
        assertTrue(r.chshVerdict().startsWith("S is above the classical bound 2"));
        assertTrue(r.settings().contains("45.00° and −45.00° from z"), r.settings());
    }

    @Test
    @DisplayName("CHSH matches the reference 2 sqrt(1 + C^2) for unequal probabilities and any phase")
    void chshReference() {
        assertEquals("2.5612", run("0.8", "0", Z, Z).chshEntangled());
        assertEquals("2.5612", run("0.8", "2.5", Z, Z).chshEntangled());
        assertEquals("2.7129", run("0.3", "-1.7", X, Y).chshEntangled());
        assertEquals("1.5617", run("0.8", "2.5", Z, Z).chshMixtureSame());
        assertTrue(run("0.8", "0", Z, Z).settings().contains("38.66° and −38.66° from z"));
    }

    @Test
    @DisplayName("the mixture never exceeds 2 and the entangled value never exceeds Tsirelson's bound")
    void bounds() {
        for (String p0 : new String[] {"0", "0.1", "0.5", "0.77", "1"}) {
            for (String phase : new String[] {"0", "1.3", "-2.9"}) {
                Result r = run(p0, phase, Z, Z);
                assertTrue(number(r.chshMixtureBest()) <= 2.0 + 1e-9);
                assertTrue(number(r.chshMixtureSame()) <= 2.0 + 1e-9);
                assertTrue(number(r.chshEntangled()) <= 2.0 * Math.sqrt(2.0) + 1e-4);
                assertEquals(r.chshEntangled(), r.chshMaximum());
            }
        }
    }

    @Test
    @DisplayName("the phase changes the joint statistics but not the mixture, the entanglement or the CHSH value")
    void phaseInvariants() {
        Result a = run("0.8", "0", X, X);
        Result b = run("0.8", "1.3", X, X);
        assertEquals(column(a.rows(), false)[0], column(b.rows(), false)[0], 0.0);
        assertEquals(a.concurrence(), b.concurrence());
        assertEquals(a.entropy(), b.entropy());
        assertEquals(a.chshEntangled(), b.chshEntangled());
        assertTrue(!a.agreeEntangled().equals(b.agreeEntangled()));
    }

    @Test
    @DisplayName("the correlation curve: 181 points, equal at 0 degrees, and at 90 degrees the entangled state keeps C while the mixture has none")
    void correlationCurve() {
        Result r = run("0.5", "0", Z, Z);
        assertEquals(CompositePresenter.CURVE_POINTS, r.entangledCurve().size());
        assertEquals(r.entangledCurve().size(), r.mixtureCurve().size());
        assertEquals(0.0, r.entangledCurve().get(0).x(), 0.0);
        assertEquals(180.0, r.entangledCurve().get(180).x(), 0.0);
        assertEquals(1.0, r.entangledCurve().get(0).y(), 1e-12);
        assertEquals(1.0, r.mixtureCurve().get(0).y(), 1e-12);
        assertEquals(1.0, r.entangledCurve().get(90).y(), 1e-12);
        assertEquals(0.0, r.mixtureCurve().get(90).y(), 1e-12);
        assertEquals(1.0, r.entangledCurve().get(45).y(), 1e-12);
        assertEquals(0.5, r.mixtureCurve().get(45).y(), 1e-12);

        Result p = run("0.8", "2.5", Z, Z);
        assertEquals(0.8, p.entangledCurve().get(90).y(), 1e-12);
        double c = Math.toRadians(30);
        assertEquals(Math.cos(c) * Math.cos(c) + 0.8 * Math.sin(c) * Math.sin(c), p.entangledCurve().get(30).y(), 1e-12);
        assertEquals(Math.cos(c) * Math.cos(c), p.mixtureCurve().get(30).y(), 1e-12);
    }

    @Test
    @DisplayName("the setup text states both descriptions")
    void setupText() {
        Result r = run("0.25", "1.5", Z, Z);
        assertTrue(r.setup().contains("|α|² = 0.2500"), r.setup());
        assertTrue(r.setup().contains("|β|² = 0.7500"), r.setup());
        assertTrue(r.setup().contains("relative phase φ = 1.5000 rad"), r.setup());
        assertTrue(r.setup().contains("Classical mixture"), r.setup());
    }

    @Test
    @DisplayName("bad input gives a message the user can act on")
    void validation() {
        assertTrue(messageOf(new Request("", "0", Z, Z), presenter).contains("is empty"));
        assertTrue(messageOf(new Request("abc", "0", Z, Z), presenter).contains("not a number"));
        assertTrue(messageOf(new Request("1.5", "0", Z, Z), presenter).contains("between 0 and 1"));
        assertTrue(messageOf(new Request("-0.1", "0", Z, Z), presenter).contains("between 0 and 1"));
        assertTrue(messageOf(new Request("0.5", "", Z, Z), presenter).contains("Relative phase is empty"));
        assertTrue(messageOf(new Request("0.5", "x", Z, Z), presenter).contains("Relative phase is not a number"));
        assertTrue(messageOf(new Request("0.5", "1e4", Z, Z), presenter).contains("−1000 and 1000"));
        assertThrows(InvalidInputException.class, () -> presenter.compute(null));
        assertThrows(InvalidInputException.class, () -> presenter.compute(new Request("0.5", "0", null, Z)));
        assertThrows(InvalidInputException.class, () -> presenter.compute(new Request("0.5", "0", Z, null)));
    }

    @Test
    @DisplayName("the returned lists are immutable")
    void immutable() {
        Result r = run("0.5", "0", Z, Z);
        assertThrows(UnsupportedOperationException.class, () -> r.rows().clear());
        assertThrows(UnsupportedOperationException.class, () -> r.entangledCurve().clear());
        assertThrows(UnsupportedOperationException.class, () -> r.mixtureCurve().clear());
    }

    @Test
    @DisplayName("probabilities that are zero up to rounding noise are shown as exactly 0 (phi = pi, X and X)")
    void noiseIsShownAsZero() {
        Result r = run("0.5", Double.toString(Math.PI), X, X);
        assertEquals("0", r.rows().get(0).entangled());
        assertEquals("0.500000", r.rows().get(1).entangled());
        assertEquals("0.500000", r.rows().get(2).entangled());
        assertEquals("0", r.rows().get(3).entangled());
        assertEquals("0", r.agreeEntangled());
    }

    @Test
    @DisplayName("a tiny amount of entanglement is still reported as entanglement and as a difference")
    void tinyEntanglementIsDetected() {
        Result r = run("0.99999999", "0", X, X);
        assertTrue(r.verdictBases().contains("different joint statistics"), r.verdictBases());
        assertTrue(r.entanglementNote().startsWith("Entangled"), r.entanglementNote());
        assertTrue(number(r.concurrence()) > 0.0001);
    }

    @Test
    @DisplayName("the detector alone is reported separately from the nucleus alone")
    void detectorAlone() {
        Result r = run("0.3", "0", Z, Z);
        assertTrue(r.alone().contains("detector |0⟩ 0.300000 (entangled) and 0.300000 (mixture)"), r.alone());
        Result x = run("0.3", "0", Z, X);
        assertTrue(x.alone().contains("detector |+⟩ 0.500000 (entangled)"), x.alone());
    }
}
