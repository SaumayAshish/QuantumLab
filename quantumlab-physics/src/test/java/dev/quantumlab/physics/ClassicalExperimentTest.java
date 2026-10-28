package dev.quantumlab.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.physics.ClassicalDecayExperiment.ClassicalRun;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClassicalExperimentTest {

    // ------------------------------------------------------------ apparatus chain

    @Test
    @DisplayName("no decay: quiet detector, armed trigger, living cat")
    void noDecayChain() {
        ApparatusOutcome o = ApparatusOutcome.consequenceOf(false);
        assertFalse(o.nucleusDecayed());
        assertEquals(DetectorStatus.NO_COUNT, o.detector());
        assertEquals(TriggerStatus.ARMED, o.trigger());
        assertEquals(CatState.ALIVE, o.cat());
    }

    @Test
    @DisplayName("decay: count, release, dead cat")
    void decayChain() {
        ApparatusOutcome o = ApparatusOutcome.consequenceOf(true);
        assertTrue(o.nucleusDecayed());
        assertEquals(DetectorStatus.COUNT, o.detector());
        assertEquals(TriggerStatus.RELEASED, o.trigger());
        assertEquals(CatState.DEAD, o.cat());
    }

    @Test
    @DisplayName("the initial apparatus is the no-decay outcome")
    void initialIsNoDecay() {
        assertEquals(ApparatusOutcome.consequenceOf(false), ApparatusOutcome.initial());
    }

    @Test
    @DisplayName("an inconsistent causal chain cannot be constructed")
    void inconsistentChainRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ApparatusOutcome(
                                false, DetectorStatus.NO_COUNT, TriggerStatus.ARMED, CatState.DEAD));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ApparatusOutcome(
                                true, DetectorStatus.NO_COUNT, TriggerStatus.RELEASED, CatState.DEAD));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ApparatusOutcome(
                                true, DetectorStatus.COUNT, TriggerStatus.ARMED, CatState.DEAD));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ApparatusOutcome(
                                true, DetectorStatus.COUNT, TriggerStatus.RELEASED, CatState.ALIVE));
    }

    // ------------------------------------------------------------ classical experiment

    @Test
    @DisplayName("a stable nucleus never decays; a zero-length window never decays")
    void neverDecays() {
        RandomGenerator rng = Reproducibility.newGenerator(1);
        for (int i = 0; i < 5000; i++) {
            assertFalse(ClassicalDecayExperiment.run(new DecayModel(0.0), 50.0, rng).outcome().nucleusDecayed());
            assertFalse(ClassicalDecayExperiment.run(new DecayModel(9.0), 0.0, rng).outcome().nucleusDecayed());
        }
    }

    @Test
    @DisplayName("an overwhelmingly unstable nucleus always decays")
    void alwaysDecays() {
        RandomGenerator rng = Reproducibility.newGenerator(2);
        for (int i = 0; i < 5000; i++) {
            ClassicalRun run = ClassicalDecayExperiment.run(new DecayModel(1e6), 1.0, rng);
            assertTrue(run.outcome().nucleusDecayed());
            assertEquals(CatState.DEAD, run.outcome().cat());
        }
    }

    @Test
    @DisplayName("the recorded draw explains the recorded outcome")
    void drawExplainsOutcome() {
        RandomGenerator rng = Reproducibility.newGenerator(3);
        DecayModel m = DecayModel.fromHalfLife(2.0);
        for (int i = 0; i < 5000; i++) {
            ClassicalRun run = ClassicalDecayExperiment.run(m, 2.0, rng);
            assertEquals(0.5, run.decayProbability(), 1e-12);
            assertEquals(run.draw() < run.decayProbability(), run.outcome().nucleusDecayed());
        }
    }

    @Test
    @DisplayName("the same seed reproduces the same runs")
    void reproducible() {
        DecayModel m = new DecayModel(0.7);
        RandomGenerator a = Reproducibility.newGenerator(55);
        RandomGenerator b = Reproducibility.newGenerator(55);
        for (int i = 0; i < 2000; i++) {
            assertEquals(
                    ClassicalDecayExperiment.run(m, 1.0, a),
                    ClassicalDecayExperiment.run(m, 1.0, b));
        }
    }

    @Test
    @DisplayName("the decay frequency approaches 1 - exp(-lambda t) (5 sigma, several seeds)")
    void frequencyMatchesLaw() {
        DecayModel m = new DecayModel(0.7);
        double t = 1.0;
        double p = m.decayProbability(t);
        int n = 200_000;
        for (long seed : new long[] {5, 6, 7}) {
            RandomGenerator rng = Reproducibility.newGenerator(seed);
            long decayed = 0;
            for (int i = 0; i < n; i++) {
                if (ClassicalDecayExperiment.run(m, t, rng).outcome().nucleusDecayed()) {
                    decayed++;
                }
            }
            double f = (double) decayed / n;
            assertTrue(
                    Math.abs(f - p) <= 5.0 * Math.sqrt(p * (1 - p) / n),
                    "seed " + seed + ": observed " + f + " vs expected " + p);
        }
    }

    @Test
    @DisplayName("an invalid observation time is rejected")
    void invalidTime() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ClassicalDecayExperiment.run(
                                new DecayModel(1.0), -1.0, Reproducibility.newGenerator(1)));
    }
}
