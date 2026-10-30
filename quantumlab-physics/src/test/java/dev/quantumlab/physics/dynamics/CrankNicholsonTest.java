package dev.quantumlab.physics.dynamics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.physics.scattering.RectangularBarrier;
import dev.quantumlab.physics.schrodinger.Grid1D;
import dev.quantumlab.physics.schrodinger.Hamiltonian1D;
import dev.quantumlab.physics.schrodinger.Potential;
import dev.quantumlab.physics.schrodinger.StationaryState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CrankNicolsonTest {

    /** Barrier of height 2 on 0 ≤ x < 1; the grid below has points exactly at 0 and 1, so it is 10 cells wide. */
    private static final Potential BARRIER = x -> x > -1e-9 && x < 1.0 - 1e-9 ? 2.0 : 0.0;

    private static final Grid1D WIDE = new Grid1D(-80.0, 80.0, 1599); // h = 0.1

    /** T(k²/2) averaged over the packet's momentum distribution |φ(k)|² ∝ exp(−2σ²(k − k0)²). */
    private static double theoreticalTransmission(double sigma, double k0, RectangularBarrier barrier) {
        int steps = 120_000;
        double lo = 0.0005;
        double hi = 6.0;
        double weighted = 0.0;
        double total = 0.0;
        for (int i = 0; i <= steps; i++) {
            double k = lo + (hi - lo) * i / steps;
            double w = Math.exp(-2.0 * sigma * sigma * (k - k0) * (k - k0));
            double edge = i == 0 || i == steps ? 0.5 : 1.0;
            weighted += edge * w * barrier.transmission(0.5 * k * k);
            total += edge * w;
        }
        return weighted / total;
    }

    @Test
    @DisplayName("the total probability stays 1, even with a time step far too large to be accurate")
    void normIsConserved() {
        Grid1D grid = new Grid1D(-40.0, 40.0, 799);
        WaveFunction1D start = GaussianPacket.create(grid, -10.0, 2.0, 1.5);
        for (double dt : new double[] {0.01, 0.2, 5.0}) {
            WaveFunction1D end = new CrankNicolson(grid, BARRIER, dt).evolve(start, 200);
            assertEquals(1.0, end.totalProbability(), 1e-10, "dt = " + dt);
        }
    }

    @Test
    @DisplayName("the mean energy is conserved while a packet hits a barrier")
    void energyIsConserved() {
        CrankNicolson cn = new CrankNicolson(WIDE, BARRIER, 0.02);
        WaveFunction1D start = GaussianPacket.create(WIDE, -20.0, 3.0, 1.5);
        double before = cn.energy(start);
        // after 1000 steps (t = 20) the packet is in the middle of the collision
        WaveFunction1D during = cn.evolve(start, 1000);
        assertEquals(before, cn.energy(during), 1e-10);
        assertTrue(during.positionSpread() > 3.0, "the packet should be distorted by the collision");
        // Continuum value k0²/2 + 1/(8σ²) = 1.1389; the grid (h = 0.1) lowers it by about 0.002.
        assertEquals(1.5 * 1.5 / 2 + 1.0 / (8 * 9.0), before, 5e-3);
    }

    @Test
    @DisplayName("an energy eigenstate keeps its density and picks up the phase 2·atan(E·dt/2) per step")
    void eigenstateOnlyRotates() {
        Grid1D grid = new Grid1D(-8.0, 8.0, 299);
        Potential oscillator = Potential.harmonic(1.0);
        StationaryState state = new Hamiltonian1D(grid, oscillator).state(4);
        double dt = 0.1;
        double theta = 2.0 * Math.atan(0.5 * dt * state.energy());
        WaveFunction1D start = new WaveFunction1D(grid, state.amplitudes(), new double[grid.interiorPoints()]);
        WaveFunction1D next = new CrankNicolson(grid, oscillator, dt).step(start);
        for (int i = 0; i < grid.interiorPoints(); i++) {
            assertEquals(state.amplitude(i) * Math.cos(theta), next.real(i), 1e-9);
            assertEquals(-state.amplitude(i) * Math.sin(theta), next.imaginary(i), 1e-9);
        }
    }

    @Test
    @DisplayName("the phase error of an eigenstate falls by 4 when the time step is halved (second order)")
    void secondOrderInTime() {
        Grid1D grid = new Grid1D(-8.0, 8.0, 299);
        Potential oscillator = Potential.harmonic(1.0);
        StationaryState state = new Hamiltonian1D(grid, oscillator).state(4);
        double[] errors = new double[3];
        double[] steps = {0.1, 0.05, 0.025};
        for (int s = 0; s < 3; s++) {
            double dt = steps[s];
            WaveFunction1D start = new WaveFunction1D(grid, state.amplitudes(), new double[grid.interiorPoints()]);
            WaveFunction1D end = new CrankNicolson(grid, oscillator, dt).evolve(start, (int) Math.round(2.0 / dt));
            double overlapRe = 0.0;
            double overlapIm = 0.0;
            for (int i = 0; i < grid.interiorPoints(); i++) {
                overlapRe += state.amplitude(i) * end.real(i);
                overlapIm += state.amplitude(i) * end.imaginary(i);
            }
            double phase = -Math.atan2(overlapIm, overlapRe);
            double exact = state.energy() * 2.0;
            double difference = phase - exact;
            difference -= 2.0 * Math.PI * Math.rint(difference / (2.0 * Math.PI));
            errors[s] = Math.abs(difference);
        }
        assertTrue(errors[0] > 0.05, "the coarsest step should show a visible phase error");
        assertEquals(4.0, errors[0] / errors[1], 0.3);
        assertEquals(4.0, errors[1] / errors[2], 0.1);
    }

    @Test
    @DisplayName("a free packet moves at k0 and spreads as sqrt(σ² + (t / 2σ)²)")
    void freePacketSpreading() {
        Grid1D grid = new Grid1D(-60.0, 60.0, 2399); // h = 0.05
        double sigma = 2.0;
        double t = 10.0;
        WaveFunction1D start = GaussianPacket.create(grid, -10.0, sigma, 1.0);
        WaveFunction1D end = new CrankNicolson(grid, Potential.zero(), 0.005).evolve(start, 2000);
        assertEquals(-10.0 + 1.0 * t, end.meanPosition(), 0.01);
        assertEquals(Math.sqrt(sigma * sigma + t * t / (4 * sigma * sigma)), end.positionSpread(), 5e-3);
        assertEquals(1.0, end.totalProbability(), 1e-10);
    }

    @Test
    @DisplayName("late-time transmission through a barrier matches the stationary T(E) averaged over the packet")
    void transmittedFractionMatchesStationaryTheory() {
        double sigma = 3.0;
        double k0 = 1.5;
        WaveFunction1D start = GaussianPacket.create(WIDE, -20.0, sigma, k0);
        WaveFunction1D end = new CrankNicolson(WIDE, BARRIER, 0.02).evolve(start, 2250); // t = 45

        double transmitted = end.probabilityBetween(1.0 - 1e-9, 200.0);
        double reflected = end.probabilityBetween(-200.0, -1e-9);
        double inside = end.probabilityBetween(-1e-9, 1.0 - 1e-9);

        double theory = theoreticalTransmission(sigma, k0, new RectangularBarrier(2.0, 1.0));
        assertEquals(0.2505, theory, 1e-4);
        assertEquals(theory, transmitted, 2e-3);
        // An independent split-step Fourier calculation (NumPy, barrier exactly 1 wide) gave 0.24999.
        assertEquals(0.24999, transmitted, 1e-3);
        assertEquals(1.0, transmitted + reflected + inside, 1e-9);
        assertTrue(inside < 1e-6, "nothing should be left inside the barrier at late time");
    }

    @Test
    @DisplayName("the transmitted fraction is the same for a finer time step: the grid, not dt, limits accuracy here")
    void timeStepBarelyMatters() {
        WaveFunction1D start = GaussianPacket.create(WIDE, -20.0, 3.0, 1.5);
        double coarse = new CrankNicolson(WIDE, BARRIER, 0.02).evolve(start, 2250).probabilityBetween(1.0 - 1e-9, 200.0);
        double fine = new CrankNicolson(WIDE, BARRIER, 0.01).evolve(start, 4500).probabilityBetween(1.0 - 1e-9, 200.0);
        assertEquals(fine, coarse, 1e-6);
    }

    @Test
    @DisplayName("invalid arguments are rejected")
    void validation() {
        Grid1D grid = new Grid1D(-10.0, 10.0, 99);
        assertThrows(IllegalArgumentException.class, () -> new CrankNicolson(null, BARRIER, 0.1));
        assertThrows(IllegalArgumentException.class, () -> new CrankNicolson(grid, null, 0.1));
        assertThrows(IllegalArgumentException.class, () -> new CrankNicolson(grid, BARRIER, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new CrankNicolson(grid, BARRIER, -0.1));
        assertThrows(IllegalArgumentException.class, () -> new CrankNicolson(grid, BARRIER, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new CrankNicolson(grid, x -> Double.NaN, 0.1));

        CrankNicolson cn = new CrankNicolson(grid, BARRIER, 0.1);
        WaveFunction1D start = GaussianPacket.create(grid, -4.0, 0.5, 1.0);
        assertThrows(IllegalArgumentException.class, () -> cn.evolve(start, -1));
        assertEquals(start.totalProbability(), cn.evolve(start, 0).totalProbability(), 0.0);
        WaveFunction1D other = GaussianPacket.create(new Grid1D(-20.0, 20.0, 99), -4.0, 1.0, 1.0);
        assertThrows(IllegalArgumentException.class, () -> cn.step(other));
        assertThrows(IllegalArgumentException.class, () -> cn.energy(other));
    }
}
