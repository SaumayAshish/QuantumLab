package dev.quantumlab.desktop;

import dev.quantumlab.math.Complex;
import dev.quantumlab.physics.QubitState;
import dev.quantumlab.physics.StandardBasis;
import dev.quantumlab.simulation.ExperimentExporter;
import dev.quantumlab.simulation.Formatting;
import dev.quantumlab.simulation.MeasurementSession;
import dev.quantumlab.simulation.QuantumMeasurementConfig;
import dev.quantumlab.simulation.TrialRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * The logic behind the measurement screen, free of any JavaFX type so it can be unit-tested.
 *
 * <p>It turns typed text into a validated experiment, runs trials, and returns immutable, formatted
 * views. A failed {@link #prepare} leaves the previous experiment untouched.
 *
 * <p>Not thread-safe: the underlying {@link MeasurementSession} is not. The UI confines a presenter
 * to a single worker thread and passes only the immutable views back to the UI thread.
 */
public final class LabPresenter {

    /**
     * Raw field contents for one experiment.
     *
     * @param alphaRe real part of α (amplitude of |0⟩)
     * @param alphaIm imaginary part of α
     * @param betaRe  real part of β (amplitude of |1⟩)
     * @param betaIm  imaginary part of β
     * @param basis   measurement basis
     * @param seed    generator seed
     */
    public record Request(
            String alphaRe,
            String alphaIm,
            String betaRe,
            String betaIm,
            StandardBasis basis,
            String seed) {

        /**
         * Fills the fields from a state, using full-precision, locale-independent numbers.
         */
        public static Request fromState(QubitState state, StandardBasis basis, String seed) {
            return new Request(
                    Double.toString(state.alpha().re()),
                    Double.toString(state.alpha().im()),
                    Double.toString(state.beta().re()),
                    Double.toString(state.beta().im()),
                    basis,
                    seed);
        }
    }

    /**
     * Entered amplitudes are rescaled silently only within this tolerance of unit length.
     */
    private static final double NORM_TOLERANCE = 1e-9;

    private MeasurementSession session;

    public boolean hasExperiment() {
        return session != null;
    }

    /**
     * Validates the request and starts a new experiment, discarding any previous trials. The
     * amplitudes are rescaled to unit length if they are not already, and the returned view says so.
     *
     * @throws InvalidInputException if any field is invalid; the previous experiment is kept
     */
    public StateView prepare(Request r) {
        if (r == null || r.basis() == null) {
            throw new InvalidInputException("Choose a measurement basis.");
        }
        Complex alpha =
                Complex.of(
                        InputParser.number(r.alphaRe(), "α (real part)"),
                        InputParser.number(r.alphaIm(), "α (imaginary part)"));
        Complex beta =
                Complex.of(
                        InputParser.number(r.betaRe(), "β (real part)"),
                        InputParser.number(r.betaIm(), "β (imaginary part)"));
        long seed = InputParser.seed(r.seed());

        double norm2 = alpha.abs2() + beta.abs2();
        if (!Double.isFinite(norm2)) {
            throw new InvalidInputException("The amplitudes are too large.");
        }
        if (norm2 == 0.0) {
            throw new InvalidInputException(
                    "α and β are both zero (or too small to represent); a state needs a non-zero"
                            + " amplitude.");
        }

        MeasurementSession fresh;
        try {
            QubitState state = QubitState.normalizing(alpha, beta);
            fresh = new MeasurementSession(QuantumMeasurementConfig.of(state, r.basis(), seed));
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException(e.getMessage());
        }
        session = fresh;

        String note = "";
        if (Math.abs(norm2 - 1.0) > NORM_TOLERANCE) {
            note =
                    "The entered amplitudes were rescaled to unit length (|α|² + |β|² was "
                            + Formatting.fixed(norm2, 4)
                            + ").";
        }
        return StateView.of(fresh, note);
    }

    /**
     * Runs the requested number of trials.
     *
     * @throws IllegalStateException if no experiment has been prepared
     * @throws InvalidInputException if the count is invalid or the trial limit has been reached
     */
    public ResultsView measure(String trialCount) {
        MeasurementSession s = requireExperiment();
        int n = InputParser.trialCount(trialCount);
        if (s.remainingCapacity() == 0) {
            throw new InvalidInputException(
                    "The trial limit of " + MeasurementSession.MAX_TRIALS + " has been reached. Reset to start over.");
        }
        s.measure(n); // stops at the limit; the view reports how many remain
        return ResultsView.of(s);
    }

    /**
     * Clears all trials and rewinds the generator to the seed, so a rerun repeats exactly.
     */
    public ResultsView reset() {
        MeasurementSession s = requireExperiment();
        s.reset();
        return ResultsView.of(s);
    }

    public ResultsView results() {
        return ResultsView.of(requireExperiment());
    }

    /**
     * The numbers behind the charts.
     *
     * @param maxPoints at most this many points (at least 2) on the convergence curve
     */
    public ChartData chartData(int maxPoints) {
        return ChartData.of(requireExperiment(), maxPoints);
    }

    /**
     * The last {@code max} trials, oldest first, one display line each.
     */
    public List<String> history(int max) {
        MeasurementSession s = requireExperiment();
        List<String> lines = new ArrayList<>();
        for (TrialRecord t : s.recentTrials(max)) {
            lines.add(
                    "#" + t.trialNumber()
                            + "  " + t.outcomeLabel()
                            + "  (p = " + Formatting.percent(t.theoreticalProbability()) + ")");
        }
        return lines;
    }

    /**
     * The full experiment record as JSON (configuration, seed, statistics, history).
     */
    public String exportJson() {
        return ExperimentExporter.toJson(requireExperiment());
    }

    /**
     * The trial history as CSV.
     */
    public String exportCsv() {
        return ExperimentExporter.historyCsv(requireExperiment());
    }

    private MeasurementSession requireExperiment() {
        if (session == null) {
            throw new IllegalStateException("No experiment has been prepared yet.");
        }
        return session;
    }
}
