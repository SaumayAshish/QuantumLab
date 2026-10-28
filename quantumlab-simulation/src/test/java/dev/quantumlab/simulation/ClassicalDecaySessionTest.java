package dev.quantumlab.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.physics.ApparatusOutcome;
import dev.quantumlab.physics.CatState;
import dev.quantumlab.physics.ClassicalDecayExperiment.ClassicalRun;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClassicalDecaySessionTest {

    private static ClassicalDecaySession session(double lambda, double t, long seed) {
        return new ClassicalDecaySession(new ClassicalDecayConfig(lambda, t, seed));
    }

    @Test
    @DisplayName("before any run the apparatus is in its initial state")
    void initialApparatus() {
        ClassicalDecaySession s = session(0.5, 1.0, 1);
        assertEquals(ApparatusOutcome.initial(), s.currentApparatus());
        assertFalse(s.lastRun().isPresent());
        assertEquals(0, s.runCount());
    }

    @Test
    @DisplayName("theoretical probabilities follow the decay law and sum to one")
    void theoreticalProbabilities() {
        ClassicalDecaySession s = session(0.5, 2.0, 1);
        double[] p = s.theoreticalProbabilities();
        assertEquals(Math.exp(-1.0), p[0], 1e-12);
        assertEquals(1.0 - Math.exp(-1.0), p[1], 1e-12);
        assertEquals(1.0, p[0] + p[1], 1e-12);
    }

    @Test
    @DisplayName("a run updates the apparatus consistently with the nucleus's fate")
    void runUpdatesApparatus() {
        ClassicalDecaySession s = session(0.7, 1.0, 11);
        for (int i = 0; i < 100; i++) {
            ClassicalRun r = s.runOnce();
            assertEquals(r.outcome(), s.currentApparatus());
            assertEquals(
                    r.outcome().nucleusDecayed(), s.currentApparatus().cat() == CatState.DEAD);
        }
        assertEquals(100, s.runCount());
        assertTrue(s.lastRun().isPresent());
    }

    @Test
    @DisplayName("same seed reproduces the same outcomes; reset replays them")
    void reproducible() {
        ClassicalDecaySession a = session(0.7, 1.0, 21);
        ClassicalDecaySession b = session(0.7, 1.0, 21);
        a.run(4000);
        b.run(4000);
        assertEquals(a.counts()[1], b.counts()[1]);

        long first = a.counts()[1];
        a.reset();
        assertEquals(0, a.runCount());
        assertFalse(a.lastRun().isPresent());
        a.run(4000);
        assertEquals(first, a.counts()[1]);
    }

    @Test
    @DisplayName("run(n) equals n calls of runOnce()")
    void batchEqualsSingles() {
        ClassicalDecaySession a = session(0.3, 2.0, 5);
        ClassicalDecaySession b = session(0.3, 2.0, 5);
        a.run(777);
        for (int i = 0; i < 777; i++) {
            b.runOnce();
        }
        assertEquals(a.counts()[1], b.counts()[1]);
    }

    @Test
    @DisplayName("observed decay frequency is within 5 sigma of the law for several seeds")
    void frequencyMatchesLaw() {
        for (long seed : new long[] {1, 2, 3, 2026}) {
            ClassicalDecaySession s = session(0.7, 1.0, seed);
            s.run(200_000);
            assertTrue(s.summary().withinFiveSigma(), "seed " + seed + ": " + s.summary().outcomes());
        }
    }

    @Test
    @DisplayName("a stable nucleus never decays")
    void stable() {
        ClassicalDecaySession s = session(0.0, 100.0, 1);
        s.run(1000);
        assertEquals(0, s.counts()[1]);
        assertEquals(1000, s.counts()[0]);
        assertEquals(CatState.ALIVE, s.currentApparatus().cat());
    }

    @Test
    @DisplayName("the run limit is enforced")
    void limit() {
        ClassicalDecaySession s = session(0.7, 1.0, 1);
        assertEquals(ClassicalDecaySession.MAX_RUNS, s.run(ClassicalDecaySession.MAX_RUNS + 10));
        assertEquals(0, s.remainingCapacity());
        assertThrows(IllegalStateException.class, s::runOnce);
        assertThrows(IllegalArgumentException.class, () -> s.run(0));
    }

    @Test
    @DisplayName("labels follow the qubit convention: 0 = not decayed, 1 = decayed")
    void labels() {
        assertEquals("not decayed", ClassicalDecaySession.LABELS.get(0));
        assertEquals("decayed", ClassicalDecaySession.LABELS.get(1));
    }
}
