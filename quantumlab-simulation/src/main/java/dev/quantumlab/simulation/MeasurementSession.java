package dev.quantumlab.simulation;

import dev.quantumlab.physics.ApparatusOutcome;
import dev.quantumlab.physics.BornRule;
import dev.quantumlab.physics.MeasurementBasis;
import dev.quantumlab.physics.ProjectiveMeasurement;
import dev.quantumlab.physics.QubitState;
import dev.quantumlab.physics.Reproducibility;
import dev.quantumlab.physics.StandardBasis;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Modes B and C: repeated projective measurement of one prepared state.
 *
 * <p><b>What a "trial" is.</b> Every trial prepares a <em>fresh copy</em> of the configured state
 * and measures it once. Trials are therefore independent draws from the same Born distribution;
 * they are not successive measurements of one collapsing state. This is what makes the tally a
 * valid estimate of the theoretical probabilities.
 *
 * <p>The session is fully determined by its {@link QuantumMeasurementConfig}: {@link #reset()}
 * rewinds the generator to the configured seed, so replaying the same number of trials reproduces
 * the same outcomes exactly.
 *
 * <p>Not thread-safe: confine a session to one thread (the JavaFX Application Thread in the
 * desktop app).
 */
public final class MeasurementSession {

    /** Upper bound on trials per session; bounds memory and keeps the UI responsive. */
    public static final int MAX_TRIALS = 1_000_000;

    private final QuantumMeasurementConfig config;
    private final QubitState state;
    private final MeasurementBasis basis;
    private final double[] born;
    private final OutcomeLog log;
    private RandomGenerator rng;

    public MeasurementSession(QuantumMeasurementConfig config) {
        this.config = config;
        this.state = config.state();
        this.basis = config.basis().basis();
        this.born = BornRule.probabilities(state, basis);
        this.log = new OutcomeLog(basis.size(), MAX_TRIALS);
        this.rng = Reproducibility.newGenerator(config.seed());
    }

    public QuantumMeasurementConfig config() {
        return config;
    }

    public QubitState preparedState() {
        return state;
    }

    public MeasurementBasis basis() {
        return basis;
    }

    /** The Born probability of each outcome, in outcome order. */
    public double[] theoreticalProbabilities() {
        return born.clone();
    }

    public List<String> labels() {
        List<String> labels = new ArrayList<>(basis.size());
        for (int k = 0; k < basis.size(); k++) {
            labels.add(basis.label(k));
        }
        return labels;
    }

    // ---------------------------------------------------------------- running

    /**
     * Performs one trial.
     *
     * @throws IllegalStateException if {@link #MAX_TRIALS} has been reached
     */
    public TrialRecord measureOnce() {
        if (log.remaining() == 0) {
            throw new IllegalStateException("Trial limit of " + MAX_TRIALS + " reached");
        }
        log.add(ProjectiveMeasurement.sampleIndex(born, rng));
        return trial(log.count() - 1);
    }

    /**
     * Performs up to {@code n} trials, stopping at {@link #MAX_TRIALS}.
     *
     * @return how many trials were actually performed
     */
    public int measure(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be >= 1, got " + n);
        }
        int todo = Math.min(n, log.remaining());
        for (int i = 0; i < todo; i++) {
            log.add(ProjectiveMeasurement.sampleIndex(born, rng));
        }
        return todo;
    }

    /** Clears all trials and rewinds the generator to the configured seed. */
    public void reset() {
        log.clear();
        rng = Reproducibility.newGenerator(config.seed());
    }

    // ---------------------------------------------------------------- results

    public int trialCount() {
        return log.count();
    }

    public int remainingCapacity() {
        return log.remaining();
    }

    public long[] counts() {
        return log.tallyCopy();
    }

    /** The outcome index of the (0-based) {@code index}-th trial. */
    public int outcomeAt(int index) {
        return log.outcomeAt(index);
    }

    /** The (0-based) {@code index}-th trial as a full record. */
    public TrialRecord trial(int index) {
        int outcome = log.outcomeAt(index);
        Optional<ApparatusOutcome> apparatus =
                config.basis() == StandardBasis.COMPUTATIONAL
                        ? Optional.of(ApparatusOutcome.consequenceOf(outcome == 1))
                        : Optional.empty();
        return new TrialRecord(
                index + 1, state, outcome, basis.label(outcome), born[outcome], apparatus);
    }

    /** The last {@code max} trials, oldest first. */
    public List<TrialRecord> recentTrials(int max) {
        if (max < 0) {
            throw new IllegalArgumentException("max must be >= 0");
        }
        int from = Math.max(0, log.count() - max);
        List<TrialRecord> out = new ArrayList<>(log.count() - from);
        for (int i = from; i < log.count(); i++) {
            out.add(trial(i));
        }
        return out;
    }

    public StatisticalSummary summary() {
        return StatisticalSummary.of(labels(), log.tallyCopy(), born);
    }

    /**
     * The running frequency of one outcome after {@code n} trials, thinned to at most {@code
     * maxPoints} points (geometrically spaced; see {@link OutcomeLog}).
     */
    public List<SeriesPoint> runningFrequency(int outcome, int maxPoints) {
        return log.runningFrequency(outcome, maxPoints);
    }
}
