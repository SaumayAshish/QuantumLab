package dev.quantumlab.simulation;

import dev.quantumlab.physics.ApparatusOutcome;
import dev.quantumlab.physics.ClassicalDecayExperiment;
import dev.quantumlab.physics.ClassicalDecayExperiment.ClassicalRun;
import dev.quantumlab.physics.DecayModel;
import dev.quantumlab.physics.Reproducibility;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Mode A: repeated runs of the classical decay experiment.
 *
 * <p>Outcome index 0 is "not decayed" and 1 is "decayed", matching the qubit basis convention.
 * Not thread-safe; confine to one thread.
 */
public final class ClassicalDecaySession {

    public static final int MAX_RUNS = 1_000_000;

    public static final List<String> LABELS = List.of("not decayed", "decayed");

    private final ClassicalDecayConfig config;
    private final DecayModel model;
    private final OutcomeLog log = new OutcomeLog(2, MAX_RUNS);
    private RandomGenerator rng;
    private ClassicalRun lastRun;

    public ClassicalDecaySession(ClassicalDecayConfig config) {
        this.config = config;
        this.model = config.model();
        this.rng = Reproducibility.newGenerator(config.seed());
    }

    public ClassicalDecayConfig config() {
        return config;
    }

    /** {@code [P(not decayed), P(decayed)]} at the configured observation time. */
    public double[] theoreticalProbabilities() {
        return new double[] {
                model.survivalProbability(config.observationTime()),
                model.decayProbability(config.observationTime())
        };
    }

    /**
     * Performs one run.
     *
     * @throws IllegalStateException if {@link #MAX_RUNS} has been reached
     */
    public ClassicalRun runOnce() {
        if (log.remaining() == 0) {
            throw new IllegalStateException("Run limit of " + MAX_RUNS + " reached");
        }
        lastRun = ClassicalDecayExperiment.run(model, config.observationTime(), rng);
        log.add(lastRun.outcome().nucleusDecayed() ? 1 : 0);
        return lastRun;
    }

    /**
     * Performs up to {@code n} runs, stopping at {@link #MAX_RUNS}.
     *
     * @return how many runs were actually performed
     */
    public int run(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be >= 1, got " + n);
        }
        int todo = Math.min(n, log.remaining());
        for (int i = 0; i < todo; i++) {
            runOnce();
        }
        return todo;
    }

    /** Clears all runs and rewinds the generator to the configured seed. */
    public void reset() {
        log.clear();
        lastRun = null;
        rng = Reproducibility.newGenerator(config.seed());
    }

    public Optional<ClassicalRun> lastRun() {
        return Optional.ofNullable(lastRun);
    }

    /** The apparatus as it stands: its initial state before any run, else the last run's record. */
    public ApparatusOutcome currentApparatus() {
        return lastRun == null ? ApparatusOutcome.initial() : lastRun.outcome();
    }

    public int runCount() {
        return log.count();
    }

    public int remainingCapacity() {
        return log.remaining();
    }

    public long[] counts() {
        return log.tallyCopy();
    }

    public StatisticalSummary summary() {
        return StatisticalSummary.of(LABELS, log.tallyCopy(), theoreticalProbabilities());
    }

    public List<SeriesPoint> runningFrequency(int outcome, int maxPoints) {
        return log.runningFrequency(outcome, maxPoints);
    }
}
