package dev.quantumlab.physics.schrodinger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Hamiltonian1DTest {

    private static final Grid1D HARMONIC_GRID = new Grid1D(-10.0, 10.0, 2000);

    @Test
    @DisplayName("on [0, 1] with 3 unknowns the matrix has 1/h² = 16 on the diagonal and −8 beside it")
    void matrixEntries() {
        Hamiltonian1D h = new Hamiltonian1D(new Grid1D(0.0, 1.0, 3), Potential.zero());
        for (int i = 0; i < 3; i++) {
            assertEquals(16.0, h.matrix().diagonal(i), 1e-12);
        }
        assertEquals(-8.0, h.matrix().offDiagonal(0), 1e-12);
        assertEquals(-8.0, h.matrix().offDiagonal(1), 1e-12);
    }

    @Test
    @DisplayName("the potential is added to the diagonal at each grid point")
    void potentialOnDiagonal() {
        Grid1D g = new Grid1D(0.0, 1.0, 3);
        Hamiltonian1D h = new Hamiltonian1D(g, x -> 10.0 * x);
        assertEquals(16.0 + 2.5, h.matrix().diagonal(0), 1e-12);
        assertEquals(16.0 + 5.0, h.matrix().diagonal(1), 1e-12);
        assertEquals(16.0 + 7.5, h.matrix().diagonal(2), 1e-12);
    }

    @Test
    @DisplayName("the infinite-well levels equal the exact finite-difference formula (2/h²) sin²(nπh/2L)")
    void wellMatchesExactDiscreteFormula() {
        InfiniteWell well = new InfiniteWell(0.0, 1.0);
        Hamiltonian1D h = new Hamiltonian1D(well.grid(200), Potential.zero());
        for (int k = 0; k < 10; k++) {
            double exact = well.discreteEnergy(k + 1, 200);
            assertEquals(exact, h.energy(k), 1e-9 * exact);
        }
    }

    @Test
    @DisplayName("the error against the continuum is second order: halving h divides it by four")
    void secondOrderConvergence() {
        InfiniteWell well = new InfiniteWell(0.0, 1.0);
        double previousError = Double.NaN;
        for (int cells : new int[] {20, 40, 80, 160}) {
            Hamiltonian1D h = new Hamiltonian1D(well.grid(cells - 1), Potential.zero());
            double error = well.energy(1) - h.energy(0);
            assertTrue(error > 0.0, "the discrete level lies below the exact one");
            if (!Double.isNaN(previousError)) {
                double ratio = previousError / error;
                assertTrue(ratio > 3.95 && ratio < 4.05, "ratio " + ratio);
            }
            previousError = error;
        }
    }

    @Test
    @DisplayName("the well's numerical states coincide with the exact wavefunctions")
    void wellStatesMatchExactWavefunctions() {
        InfiniteWell well = new InfiniteWell(0.0, 1.0);
        Grid1D grid = well.grid(150);
        Hamiltonian1D h = new Hamiltonian1D(grid, Potential.zero());
        for (int k = 0; k < 3; k++) {
            StationaryState s = h.state(k);
            for (int i = 0; i < 150; i++) {
                assertEquals(well.wavefunction(k + 1, grid.x(i)), s.amplitude(i), 1e-9);
            }
        }
    }

    @Test
    @DisplayName("harmonic oscillator levels agree with values computed independently by NumPy")
    void harmonicMatchesNumpy() {
        // numpy.linalg.eigh on the same matrix (grid [-10, 10], 2000 unknowns, omega = 1).
        double[] reference = {
            0.4999968781026011, 1.4999843904377994, 2.499959414912327,
            3.499921951292231, 4.499871999344744, 5.499809558834519
        };
        Hamiltonian1D h = new Hamiltonian1D(HARMONIC_GRID, Potential.harmonic(1.0));
        for (int k = 0; k < reference.length; k++) {
            assertEquals(reference[k], h.energy(k), 1e-9);
        }
    }

    @Test
    @DisplayName("the harmonic oscillator error against ω(n + ½) shrinks like h²")
    void harmonicConvergence() {
        double coarse = 0.5 - new Hamiltonian1D(new Grid1D(-10.0, 10.0, 800), Potential.harmonic(1.0)).energy(0);
        double fine = 0.5 - new Hamiltonian1D(HARMONIC_GRID, Potential.harmonic(1.0)).energy(0);
        assertTrue(coarse > 0.0 && fine > 0.0);
        double expectedRatio = Math.pow(2001.0 / 801.0, 2);
        assertEquals(expectedRatio, coarse / fine, 0.4);
    }

    @Test
    @DisplayName("the harmonic ground state matches the exact Gaussian π^(−1/4) e^(−x²/2)")
    void harmonicGroundStateIsGaussian() {
        StationaryState s = new Hamiltonian1D(HARMONIC_GRID, Potential.harmonic(1.0)).state(0);
        double worst = 0.0;
        for (int i = 0; i < HARMONIC_GRID.interiorPoints(); i++) {
            double x = HARMONIC_GRID.x(i);
            worst = Math.max(worst, Math.abs(s.amplitude(i) - Math.pow(Math.PI, -0.25) * Math.exp(-x * x / 2.0)));
        }
        assertTrue(worst < 1e-5, "max deviation " + worst);
    }

    @Test
    @DisplayName("states are normalised so that Σ ψ² h = 1")
    void normalisation() {
        Hamiltonian1D h = new Hamiltonian1D(HARMONIC_GRID, Potential.harmonic(1.0));
        for (int k = 0; k < 4; k++) {
            assertEquals(1.0, h.state(k).probability(0, 2000), 1e-9);
        }
    }

    @Test
    @DisplayName("level n has n nodes and parity (−1)ⁿ in a symmetric potential")
    void nodesAndParity() {
        Hamiltonian1D h = new Hamiltonian1D(HARMONIC_GRID, Potential.harmonic(1.0));
        int n = HARMONIC_GRID.interiorPoints();
        for (int k = 0; k < 6; k++) {
            StationaryState s = h.state(k);
            assertEquals(k, s.nodes(), "nodes of level " + k);
            double sign = (k % 2 == 0) ? 1.0 : -1.0;
            for (int i = 0; i < n; i++) {
                assertEquals(sign * s.amplitude(i), s.amplitude(n - 1 - i), 1e-8);
            }
        }
        assertEquals(0.5, h.state(0).probability(0, n / 2), 1e-9);
    }

    @Test
    @DisplayName("adding a constant to the potential shifts every energy and changes no state")
    void constantShift() {
        Grid1D g = new Grid1D(-5.0, 5.0, 300);
        Hamiltonian1D base = new Hamiltonian1D(g, Potential.harmonic(1.0));
        Hamiltonian1D shifted = new Hamiltonian1D(g, x -> 0.5 * x * x + 7.0);
        for (int k = 0; k < 4; k++) {
            assertEquals(base.energy(k) + 7.0, shifted.energy(k), 1e-9);
            for (int i = 0; i < 300; i += 17) {
                assertEquals(base.state(k).amplitude(i), shifted.state(k).amplitude(i), 1e-7);
            }
        }
    }

    @Test
    @DisplayName("energies ascend and energies(count) agrees with energy(k)")
    void ascending() {
        Hamiltonian1D h = new Hamiltonian1D(new Grid1D(-8.0, 8.0, 400), Potential.harmonic(1.0));
        double[] e = h.energies(8);
        for (int k = 0; k < 8; k++) {
            assertEquals(h.energy(k), e[k], 0.0);
            if (k > 0) {
                assertTrue(e[k] > e[k - 1]);
            }
        }
    }

    @Test
    @DisplayName("a state hands out copies, so its amplitudes cannot be changed from outside")
    void immutableState() {
        StationaryState s = new Hamiltonian1D(new Grid1D(0.0, 1.0, 50), Potential.zero()).state(0);
        double before = s.amplitude(10);
        s.amplitudes()[10] = 123.0;
        assertEquals(before, s.amplitude(10), 0.0);
        assertEquals(s.amplitude(10) * s.amplitude(10), s.density(10), 1e-15);
    }

    @Test
    @DisplayName("invalid arguments are rejected, including a potential that is not finite")
    void validation() {
        Grid1D g = new Grid1D(0.0, 1.0, 10);
        assertThrows(IllegalArgumentException.class, () -> new Hamiltonian1D(null, Potential.zero()));
        assertThrows(IllegalArgumentException.class, () -> new Hamiltonian1D(g, null));
        assertThrows(IllegalArgumentException.class, () -> new Hamiltonian1D(g, x -> Double.NaN));
        assertThrows(
            IllegalArgumentException.class,
            () -> new Hamiltonian1D(g, x -> x > 0.4 ? Double.POSITIVE_INFINITY : 0.0));
        assertThrows(IllegalArgumentException.class, () -> Potential.harmonic(0.0));
        assertThrows(IllegalArgumentException.class, () -> Potential.harmonic(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Potential.constant(Double.POSITIVE_INFINITY));
        Hamiltonian1D h = new Hamiltonian1D(g, Potential.zero());
        assertThrows(IllegalArgumentException.class, () -> h.energy(10));
        assertThrows(IllegalArgumentException.class, () -> h.state(-1));
        StationaryState s = h.state(0);
        assertThrows(IllegalArgumentException.class, () -> s.probability(5, 3));
        assertThrows(IllegalArgumentException.class, () -> s.probability(0, 11));
    }
}
