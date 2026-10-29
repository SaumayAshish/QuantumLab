package dev.quantumlab.desktop;

import dev.quantumlab.physics.ApparatusOutcome;
import dev.quantumlab.physics.ClassicalDecayExperiment.ClassicalRun;
import dev.quantumlab.simulation.ClassicalDecayConfig;
import dev.quantumlab.simulation.ClassicalDecaySession;
import dev.quantumlab.simulation.ExperimentExporter;
import dev.quantumlab.simulation.Formatting;

/**
 * The logic behind the classical decay screen (Mode A), free of any JavaFX type.
 *
 * <p>In this mode the nucleus, detector, trigger and cat each have a definite state throughout;
 * the randomness is ignorance of a definite outcome, not superposition. Not thread-safe: confine
 * to one worker thread, as for {@link LabPresenter}.
 */
public final class ClassicalPresenter {

    /** Raw field contents for one experiment. */
    public record Request(String decayRate, String observationTime, String seed) {}

    /**
     * The configured decay law, formatted.
     *
     * @param decayRate λ
     * @param observationTime t
     * @param halfLife ln 2 / λ, or a note that the nucleus is stable
     * @param decayProbability P(decayed by t)
     * @param survivalProbability P(not decayed by t)
     */
    public record SetupView(
            String decayRate,
            String observationTime,
            String halfLife,
            String decayProbability,
            String survivalProbability) {}

    /** The apparatus chain, one plain-language state per link. */
    public record ApparatusView(String nucleus, String detector, String trigger, String cat) {

        static ApparatusView of(ApparatusOutcome o) {
            return new ApparatusView(
                    o.nucleusDecayed() ? "decayed" : "intact",
                    switch (o.detector()) {
                        case NO_COUNT -> "no count";
                        case COUNT -> "count registered";
                    },
                    switch (o.trigger()) {
                        case ARMED -> "armed";
                        case RELEASED -> "released";
                    },
                    switch (o.cat()) {
                        case ALIVE -> "alive";
                        case DEAD -> "dead";
                    });
        }
    }

    private ClassicalDecaySession session;

    public boolean hasExperiment() {
        return session != null;
    }

    /**
     * Validates the request and starts a new experiment, discarding any previous runs.
     *
     * @throws InvalidInputException if a field is invalid; the previous experiment is kept
     */
    public SetupView prepare(Request r) {
        if (r == null) {
            throw new InvalidInputException("No settings were given.");
        }
        double rate = InputParser.number(r.decayRate(), "Decay rate λ");
        double time = InputParser.number(r.observationTime(), "Observation time t");
        long seed = InputParser.seed(r.seed());
        if (rate < 0.0) {
            throw new InvalidInputException("Decay rate λ must be zero or positive.");
        }
        if (time < 0.0) {
            throw new InvalidInputException("Observation time t must be zero or positive.");
        }

        ClassicalDecaySession fresh;
        try {
            fresh = new ClassicalDecaySession(new ClassicalDecayConfig(rate, time, seed));
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException(e.getMessage());
        }
        session = fresh;

        ClassicalDecayConfig c = fresh.config();
        double halfLife = c.model().halfLife();
        double[] born = fresh.theoreticalProbabilities();
        return new SetupView(
                Formatting.fixed(c.decayRate(), 4),
                Formatting.fixed(c.observationTime(), 4),
                Double.isInfinite(halfLife) ? "∞ (stable)" : Formatting.fixed(halfLife, 4),
                Formatting.percent(born[1]),
                Formatting.percent(born[0]));
    }

    /** Performs a single run and returns the resulting apparatus chain. */
    public ApparatusView runOnce() {
        ClassicalDecaySession s = requireExperiment();
        if (s.remainingCapacity() == 0) {
            throw new InvalidInputException(limitMessage());
        }
        s.runOnce();
        return ApparatusView.of(s.currentApparatus());
    }

    /**
     * Performs the requested number of runs.
     *
     * @throws InvalidInputException if the count is invalid or the run limit has been reached
     */
    public ResultsView run(String runCount) {
        ClassicalDecaySession s = requireExperiment();
        int n = InputParser.trialCount(runCount);
        if (s.remainingCapacity() == 0) {
            throw new InvalidInputException(limitMessage());
        }
        s.run(n);
        return ResultsView.of(s.summary(), s.remainingCapacity());
    }

    /** Clears all runs and rewinds the generator to the seed. */
    public ResultsView reset() {
        ClassicalDecaySession s = requireExperiment();
        s.reset();
        return ResultsView.of(s.summary(), s.remainingCapacity());
    }

    public ResultsView results() {
        ClassicalDecaySession s = requireExperiment();
        return ResultsView.of(s.summary(), s.remainingCapacity());
    }

    /** The apparatus as it stands: its initial state before any run, else the last run's record. */
    public ApparatusView apparatus() {
        return ApparatusView.of(requireExperiment().currentApparatus());
    }

    /** Shows how the last run's random draw decided the nucleus's fate. */
    public String lastDraw() {
        ClassicalDecaySession s = requireExperiment();
        if (s.lastRun().isEmpty()) {
            return "No run yet.";
        }
        ClassicalRun run = s.lastRun().get();
        boolean decayed = run.outcome().nucleusDecayed();
        return "Random draw u = " + Formatting.fixed(run.draw(), 4)
                + (decayed ? " < " : " ≥ ")
                + "P(decayed) = " + Formatting.fixed(run.decayProbability(), 4)
                + "  →  nucleus " + (decayed ? "decayed" : "intact");
    }

    /** The full experiment record as JSON. */
    public String exportJson() {
        return ExperimentExporter.toJson(requireExperiment());
    }

    private static String limitMessage() {
        return "The run limit of " + ClassicalDecaySession.MAX_RUNS + " has been reached. Reset to start over.";
    }

    private ClassicalDecaySession requireExperiment() {
        if (session == null) {
            throw new IllegalStateException("No experiment has been prepared yet.");
        }
        return session;
    }
}
