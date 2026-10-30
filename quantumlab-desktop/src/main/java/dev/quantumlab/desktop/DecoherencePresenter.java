package dev.quantumlab.desktop;

import dev.quantumlab.math.Complex;
import dev.quantumlab.physics.MeasurementBasis;
import dev.quantumlab.physics.QubitState;
import dev.quantumlab.physics.decoherence.Dephasing;
import dev.quantumlab.physics.decoherence.DensityMatrix;
import dev.quantumlab.simulation.Formatting;
import dev.quantumlab.simulation.SeriesPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * The logic behind the decoherence screen (Mode E), free of any JavaFX type. A qubit starts in the
 * pure state {@code α|0⟩ + β|1⟩}; pure dephasing at a chosen rate multiplies its coherence by
 * {@code e^{−γt}} and leaves the populations alone. The presenter returns pre-formatted views of the
 * statistics in a chosen basis at {@code t = 0}, at the chosen time and in the long-time limit,
 * together with the interference fringe and the purity over time. Stateless, so it may be used from
 * any thread.
 *
 * <p>Every number is an exact probability from the model. The dephasing rate is an input, not
 * something derived from an environment; nothing is sampled and no outcome is singled out.
 */
public final class DecoherencePresenter {

    public static final int TIME_POINTS = 101;
    public static final int FRINGE_POINTS = 181;

    private static final double MAX_PHASE = 1_000.0;
    private static final double MAX_RATE = 1_000.0;
    private static final double MAX_TIME = 1_000.0;
    private static final double NOISE = 1e-12;
    private static final double NEARLY_ALL_LEFT = 0.99;
    private static final double NEARLY_ALL_GONE = 0.01;
    /** The time axis covers this many coherence times, unless the chosen time is later. */
    private static final double COHERENCE_TIMES_SHOWN = 5.0;

    /** Raw field contents and the chosen measurement basis. */
    public record Request(
        String notDecayed,
        String phase,
        String rate,
        String time,
        CompositePresenter.BasisChoice basis) {}

    /** The chance of one outcome at t = 0, at the chosen time, and in the long-time limit. */
    public record RowView(String outcome, String initial, String now, String limit) {}

    /**
     * @param timeSpan the last time shown on the time axis
     * @param visibilityCurve fringe visibility {@code 2|ρ₀₁|} against time
     * @param purityCurve {@code Tr ρ²} against time
     * @param entropyCurve entropy in bits against time
     * @param fringeInitial probability of {@code (|0⟩ + e^{iφ}|1⟩)/√2} against the analyser angle φ in
     *     degrees, at t = 0
     * @param fringeNow the same at the chosen time
     */
    public record Result(
        String setup,
        String basis,
        List<RowView> rows,
        String zChances,
        String factor,
        String coherenceTime,
        String visibilityInitial,
        String visibilityNow,
        String purityInitial,
        String purityNow,
        String entropyInitial,
        String entropyNow,
        String bloch,
        String verdict,
        double timeSpan,
        List<SeriesPoint> visibilityCurve,
        List<SeriesPoint> purityCurve,
        List<SeriesPoint> entropyCurve,
        List<SeriesPoint> fringeInitial,
        List<SeriesPoint> fringeNow) {}

    public Result compute(Request r) {
        if (r == null || r.basis() == null) {
            throw new InvalidInputException("No settings were given.");
        }
        double p0 = InputParser.number(r.notDecayed(), "Probability of \"not decayed\"");
        if (!(p0 >= 0.0 && p0 <= 1.0)) {
            throw new InvalidInputException("Probability of \"not decayed\" must be between 0 and 1.");
        }
        double phi = InputParser.number(r.phase(), "Relative phase");
        if (Math.abs(phi) > MAX_PHASE) {
            throw new InvalidInputException(
                "Relative phase must be between −1000 and 1000 radians.");
        }
        double rate = InputParser.number(r.rate(), "Dephasing rate");
        if (!(rate >= 0.0 && rate <= MAX_RATE)) {
            throw new InvalidInputException("Dephasing rate must be between 0 and 1000.");
        }
        double time = InputParser.number(r.time(), "Time");
        if (!(time >= 0.0 && time <= MAX_TIME)) {
            throw new InvalidInputException("Time must be between 0 and 1000.");
        }

        double amplitude1 = Math.sqrt(1.0 - p0);
        QubitState start =
            QubitState.of(
                Complex.of(Math.sqrt(p0), 0.0),
                Complex.of(amplitude1 * Math.cos(phi), amplitude1 * Math.sin(phi)));
        DensityMatrix initial = DensityMatrix.fromState(start);
        Dephasing dephasing = new Dephasing(rate);
        DensityMatrix now = dephasing.apply(initial, time);
        DensityMatrix limit = Dephasing.fullyDephased(initial);
        double factor = dephasing.coherenceFactor(time);

        MeasurementBasis basis = r.basis().basis();
        double[] pi = initial.probabilities(basis);
        double[] pn = now.probabilities(basis);
        double[] pl = limit.probabilities(basis);
        List<RowView> rows = new ArrayList<>();
        for (int k = 0; k < 2; k++) {
            rows.add(
                new RowView(
                    "nucleus " + basis.label(k),
                    probability(pi[k]),
                    probability(pn[k]),
                    probability(pl[k])));
        }

        String zChances =
            "Chances of \"not decayed\" / \"decayed\" in the Z basis: "
                + probability(initial.population0())
                + " / "
                + probability(initial.population1())
                + " at t = 0, and "
                + probability(now.population0())
                + " / "
                + probability(now.population1())
                + " at the chosen time.";

        String coherenceTime =
            rate == 0.0
                ? "With a rate of 0 the coherence never decays."
                : "Coherence time 1/γ = " + Formatting.fixed(dephasing.coherenceTime(), 4) + ".";

        String verdict;
        if (initial.coherenceMagnitude() < NOISE) {
            verdict =
                "This starting state has no coherence between \"not decayed\" and \"decayed\" "
                    + "to lose, so dephasing changes nothing.";
        } else if (factor >= NEARLY_ALL_LEFT) {
            verdict =
                "Almost all of the coherence is still there: the statistics are close to those "
                    + "of the pure superposition.";
        } else if (factor <= NEARLY_ALL_GONE) {
            verdict =
                "Almost all of the coherence is gone: in every basis the statistics are now close "
                    + "to those of a classical mixture of \"not decayed\" and \"decayed\". The "
                    + "qubit still has these probabilities; that is not the same as one "
                    + "outcome having happened.";
        } else {
            verdict =
                "Part of the coherence is gone: "
                    + Formatting.fixed(factor * 100.0, 1)
                    + "% of it is left, so the interference fringe is correspondingly weaker "
                    + "and the statistics lie between the pure superposition and the "
                    + "classical mixture.";
        }

        double span = timeSpan(dephasing, time);
        List<SeriesPoint> visibility = new ArrayList<>(TIME_POINTS);
        List<SeriesPoint> purity = new ArrayList<>(TIME_POINTS);
        List<SeriesPoint> entropy = new ArrayList<>(TIME_POINTS);
        for (int k = 0; k < TIME_POINTS; k++) {
            double t = span * k / (TIME_POINTS - 1);
            DensityMatrix rho = dephasing.apply(initial, t);
            visibility.add(new SeriesPoint(t, rho.fringeVisibility()));
            purity.add(new SeriesPoint(t, rho.purity()));
            entropy.add(new SeriesPoint(t, rho.entropyBits()));
        }
        List<SeriesPoint> fringeInitial = new ArrayList<>(FRINGE_POINTS);
        List<SeriesPoint> fringeNow = new ArrayList<>(FRINGE_POINTS);
        for (int k = 0; k < FRINGE_POINTS; k++) {
            double angle = 2.0 * k;
            fringeInitial.add(new SeriesPoint(angle, initial.equatorialProbability(Math.toRadians(angle))));
            fringeNow.add(new SeriesPoint(angle, now.equatorialProbability(Math.toRadians(angle))));
        }

        String setup =
            "Start: α|0⟩ + β|1⟩ with |α|² = "
                + Formatting.fixed(p0, 4)
                + ", |β|² = "
                + Formatting.fixed(1.0 - p0, 4)
                + ", relative phase φ = "
                + Formatting.fixed(phi, 4)
                + " rad. Dephasing rate γ = "
                + Formatting.fixed(rate, 4)
                + ", time t = "
                + Formatting.fixed(time, 4)
                + ", so the coherence is multiplied by e^(−γt) = "
                + exact(factor)
                + ".";
        return new Result(
            setup,
            "Nucleus measured in " + basis.name() + ".",
            List.copyOf(rows),
            zChances,
            exact(factor),
            coherenceTime,
            exact(initial.fringeVisibility()),
            exact(now.fringeVisibility()),
            Formatting.fixed(initial.purity(), 6),
            Formatting.fixed(now.purity(), 6),
            Formatting.fixed(initial.entropyBits(), 6) + " bits",
            Formatting.fixed(now.entropyBits(), 6) + " bits",
            "("
                + Formatting.fixed(now.blochX(), 4)
                + ", "
                + Formatting.fixed(now.blochY(), 4)
                + ", "
                + Formatting.fixed(now.blochZ(), 4)
                + ")",
            verdict,
            span,
            List.copyOf(visibility),
            List.copyOf(purity),
            List.copyOf(entropy),
            List.copyOf(fringeInitial),
            List.copyOf(fringeNow));
    }

    /** Five coherence times, or the chosen time if that is later, and at least 1 when nothing decays. */
    static double timeSpan(Dephasing dephasing, double time) {
        double natural =
            dephasing.rate() == 0.0 ? 1.0 : COHERENCE_TIMES_SHOWN * dephasing.coherenceTime();
        return Math.max(natural, time);
    }

    /**
     * An outcome probability: fixed decimals, scientific notation when small but not zero, and exact
     * zero below rounding noise, so that a residue such as −5e−17 is never shown as a result.
     */
    static String probability(double p) {
        return TunnellingPresenter.probability(Math.abs(p) < NOISE ? 0.0 : p);
    }

    /** A derived quantity that really can be tiny, such as e^(−100): shown as it is, not rounded to 0. */
    private static String exact(double x) {
        return TunnellingPresenter.probability(x);
    }
}
