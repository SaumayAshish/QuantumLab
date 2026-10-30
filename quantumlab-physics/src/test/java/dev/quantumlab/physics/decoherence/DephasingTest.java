package dev.quantumlab.physics.decoherence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.math.Complex;
import dev.quantumlab.physics.MeasurementBasis;
import dev.quantumlab.physics.QubitState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DephasingTest {

    private static final double TOL = 1e-12;
    private static final MeasurementBasis Z = MeasurementBasis.computational();
    private static final MeasurementBasis X = MeasurementBasis.hadamard();
    private static final MeasurementBasis Y = MeasurementBasis.circular();

    private static DensityMatrix start() {
        return DensityMatrix.fromState(QubitState.of(Complex.of(0.6), Complex.of(0.0, 0.8)));
    }

    @Test
    @DisplayName("the rate must be a finite number, zero or more")
    void rateValidation() {
        assertThrows(IllegalArgumentException.class, () -> new Dephasing(-0.1));
        assertThrows(IllegalArgumentException.class, () -> new Dephasing(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new Dephasing(Double.POSITIVE_INFINITY));
        assertEquals(0.0, new Dephasing(0.0).rate(), 0.0);
    }

    @Test
    @DisplayName("the time must be a finite number, zero or more")
    void timeValidation() {
        Dephasing d = new Dephasing(1.0);
        assertThrows(IllegalArgumentException.class, () -> d.coherenceFactor(-1.0));
        assertThrows(IllegalArgumentException.class, () -> d.apply(start(), Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> d.applyByRandomFlips(start(), -0.5));
    }

    @Test
    @DisplayName("the coherence is multiplied by exp(−γt)")
    void coherenceDecay() {
        Dephasing d = new Dephasing(0.5);
        assertEquals(Math.exp(-1.0), d.coherenceFactor(2.0), TOL);
        DensityMatrix after = d.apply(start(), 2.0);
        assertEquals(0.0, after.coherence().re(), TOL);
        assertEquals(-0.17658213176229232, after.coherence().im(), 1e-15);
    }

    @Test
    @DisplayName("populations never change")
    void populationsFixed() {
        Dephasing d = new Dephasing(0.7);
        for (double t : new double[] {0.0, 0.3, 2.0, 40.0}) {
            DensityMatrix after = d.apply(start(), t);
            assertEquals(0.36, after.population0(), TOL);
            assertEquals(0.64, after.population1(), TOL);
        }
    }

    @Test
    @DisplayName("Z-basis statistics are unchanged while X and Y statistics move")
    void basisStatistics() {
        DensityMatrix before = start();
        DensityMatrix after = new Dephasing(0.5).apply(before, 2.0);
        assertArrayEquals(before.probabilities(Z), after.probabilities(Z), TOL);
        // Independent NumPy values for rho01 = -0.48i e^{-1}.
        assertArrayEquals(new double[] {0.5, 0.5}, after.probabilities(X), 1e-15);
        assertArrayEquals(new double[] {0.6765821317622922, 0.3234178682377077}, after.probabilities(Y), 1e-14);
        // Before dephasing the Y statistics were 0.98 / 0.02.
        assertArrayEquals(new double[] {0.98, 0.02}, before.probabilities(Y), 1e-14);
    }

    @Test
    @DisplayName("purity, entropy, Bloch components and visibility match independent values")
    void derivedQuantities() {
        DensityMatrix after = new Dephasing(0.5).apply(start(), 2.0);
        assertEquals(0.6015624985154313, after.purity(), 1e-14);
        assertEquals(0.8480625116545679, after.entropyBits(), 1e-14);
        assertEquals(0.35316426352458463, after.blochY(), 1e-14);
        assertEquals(-0.28, after.blochZ(), 1e-14);
        assertEquals(0.35316426352458463, after.fringeVisibility(), 1e-14);
        assertEquals(0.6137573325314052, after.equatorialProbability(0.7), 1e-14);
    }

    @Test
    @DisplayName("applying the decay in two halves equals applying it at once")
    void semigroup() {
        Dephasing d = new Dephasing(0.9);
        DensityMatrix twice = d.apply(d.apply(start(), 0.8), 1.7);
        DensityMatrix once = d.apply(start(), 2.5);
        assertTrue(twice.matrix().approxEquals(once.matrix(), 1e-14));
    }

    @Test
    @DisplayName("exp(−γt) decay equals the random Z-flip description")
    void equivalentToRandomFlips() {
        Dephasing d = new Dephasing(0.5);
        for (double t : new double[] {0.0, 0.4, 2.0, 9.0}) {
            assertTrue(
                d.apply(start(), t).matrix().approxEquals(d.applyByRandomFlips(start(), t).matrix(), 1e-14),
                "t = " + t);
        }
    }

    @Test
    @DisplayName("purity and visibility only fall, entropy only rises")
    void monotone() {
        Dephasing d = new Dephasing(0.4);
        double purity = 2.0;
        double visibility = 2.0;
        double entropy = -1.0;
        for (int k = 0; k <= 60; k++) {
            DensityMatrix rho = d.apply(start(), 0.25 * k);
            assertTrue(rho.purity() <= purity + 1e-12);
            assertTrue(rho.fringeVisibility() <= visibility + 1e-12);
            assertTrue(rho.entropyBits() >= entropy - 1e-12);
            purity = rho.purity();
            visibility = rho.fringeVisibility();
            entropy = rho.entropyBits();
        }
    }

    @Test
    @DisplayName("at long times the state approaches the classical mixture of the populations")
    void longTimeLimit() {
        Dephasing d = new Dephasing(1.0);
        DensityMatrix limit = Dephasing.fullyDephased(start());
        assertEquals(0.0, limit.coherenceMagnitude(), 0.0);
        assertEquals(0.36, limit.population0(), TOL);
        assertEquals(0.64, limit.population1(), TOL);
        assertTrue(d.apply(start(), 60.0).matrix().approxEquals(limit.matrix(), 1e-12));
        assertArrayEquals(new double[] {0.5, 0.5}, limit.probabilities(Y), TOL);
    }

    @Test
    @DisplayName("a zero rate changes nothing, and a plain mixture is already dephased")
    void trivialCases() {
        assertTrue(new Dephasing(0.0).apply(start(), 100.0).matrix().approxEquals(start().matrix(), 0.0));
        DensityMatrix mixed = DensityMatrix.maximallyMixed();
        assertTrue(new Dephasing(2.0).apply(mixed, 3.0).matrix().approxEquals(mixed.matrix(), 0.0));
        // |0⟩ has no coherence to lose.
        DensityMatrix zero = DensityMatrix.fromState(QubitState.basis0());
        assertTrue(new Dephasing(2.0).apply(zero, 3.0).matrix().approxEquals(zero.matrix(), 0.0));
    }

    @Test
    @DisplayName("coherence time is 1/γ and timeToFraction inverts the decay")
    void timescales() {
        Dephasing d = new Dephasing(0.5);
        assertEquals(2.0, d.coherenceTime(), TOL);
        assertEquals(Math.log(2.0) / 0.5, d.timeToFraction(0.5), TOL);
        assertEquals(0.25, d.coherenceFactor(d.timeToFraction(0.25)), TOL);
        assertEquals(0.0, d.timeToFraction(1.0), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, new Dephasing(0.0).coherenceTime(), 0.0);
        assertEquals(0.0, new Dephasing(0.0).timeToFraction(1.0), 0.0);
        assertThrows(IllegalArgumentException.class, () -> new Dephasing(0.0).timeToFraction(0.5));
        assertThrows(IllegalArgumentException.class, () -> d.timeToFraction(0.0));
        assertThrows(IllegalArgumentException.class, () -> d.timeToFraction(1.5));
        assertThrows(IllegalArgumentException.class, () -> d.timeToFraction(Double.NaN));
    }
}
