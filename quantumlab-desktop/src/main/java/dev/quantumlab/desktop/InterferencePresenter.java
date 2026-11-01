package dev.quantumlab.desktop;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexMatrix;
import dev.quantumlab.physics.StandardBasis;
import dev.quantumlab.physics.decoherence.DensityMatrix;
import dev.quantumlab.simulation.Formatting;
import dev.quantumlab.simulation.InterferenceCurve;
import dev.quantumlab.simulation.SeriesPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * The logic behind the interference screen, free of any JavaFX type.
 *
 * <p>One qubit with fixed chances of "not decayed" and "decayed" and a relative phase that is swept
 * through a full turn, compared with the classical mixture that has the same chances. Every number
 * comes from {@link InterferenceCurve} or from the density-matrix Born rule; nothing is sampled.
 * Stateless, so it can be used from any one thread.
 */
public final class InterferencePresenter {

    /** One sample per degree from −180° to 180°, so 0°, 90° and 180° are sampled exactly. */
    static final int POINTS = 361;

    /** Raw field contents. */
    public record Request(String notDecayed, StandardBasis basis) {}

    /**
     * One outcome of the chosen basis.
     *
     * @param outcome the outcome's name
     * @param atZero its probability at φ = 0°
     * @param atQuarter its probability at φ = 90°
     * @param atHalf its probability at φ = 180°
     * @param mixture its probability for the classical mixture, which does not depend on φ
     */
    public record RowView(
        String outcome, String atZero, String atQuarter, String atHalf, String mixture) {}

    /**
     * Everything the screen shows.
     *
     * @param setup the chosen chances and amplitude magnitudes
     * @param basis the chosen basis and which outcome is plotted
     * @param rows one row per outcome
     * @param height largest minus smallest probability of the plotted outcome over φ
     * @param expectedHeight the closed-form height: 2|α||β| in the X and Y bases, 0 in Z
     * @param verdict a plain-language reading of the curve
     * @param plotted the name of the plotted outcome
     * @param superposition the coherent superposition's curve, φ in degrees
     * @param mixture the classical mixture's curve, φ in degrees
     */
    public record Result(
        String setup,
        String basis,
        List<RowView> rows,
        String height,
        String expectedHeight,
        String verdict,
        String plotted,
        List<SeriesPoint> superposition,
        List<SeriesPoint> mixture) {}

    /**
     * Computes the curve and the table.
     *
     * @throws InvalidInputException if the chance is not a number between 0 and 1, or no basis was
     *     chosen
     */
    public Result compute(Request r) {
        if (r == null) {
            throw new InvalidInputException("No settings were given.");
        }
        if (r.basis() == null) {
            throw new InvalidInputException("Choose a measurement basis.");
        }
        double p0 = InputParser.number(r.notDecayed(), "P(not decayed)");
        if (p0 < 0.0 || p0 > 1.0) {
            throw new InvalidInputException("P(not decayed) must be between 0 and 1.");
        }
        StandardBasis basis = r.basis();
        double alpha = Math.sqrt(p0);
        double beta = Math.sqrt(1.0 - p0);

        List<SeriesPoint> first;
        List<SeriesPoint> second;
        double[] mixed;
        try {
            first = InterferenceCurve.sweep(alpha, beta, basis, 0, POINTS);
            second = InterferenceCurve.sweep(alpha, beta, basis, 1, POINTS);
            mixed = DensityMatrix.of(diagonal(p0)).probabilities(basis.basis());
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException(e.getMessage());
        }

        List<String> names = outcomeNames(basis);
        int zero = (POINTS - 1) / 2;
        int quarter = 3 * (POINTS - 1) / 4;
        int half = POINTS - 1;
        List<RowView> rows = new ArrayList<>(2);
        List<List<SeriesPoint>> curves = List.of(first, second);
        for (int k = 0; k < 2; k++) {
            List<SeriesPoint> c = curves.get(k);
            rows.add(
                new RowView(
                    names.get(k),
                    Formatting.percent(c.get(zero).y()),
                    Formatting.percent(c.get(quarter).y()),
                    Formatting.percent(c.get(half).y()),
                    Formatting.percent(mixed[k])));
        }

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (SeriesPoint p : first) {
            min = Math.min(min, p.y());
            max = Math.max(max, p.y());
        }
        double height = max - min;
        double expected = basis == StandardBasis.COMPUTATIONAL ? 0.0 : 2.0 * alpha * beta;

        String verdict;
        if (height < 1e-9) {
            verdict =
                "In this basis the probability is the same for every phase: there is no "
                    + "interference. The classical mixture gives the same answer.";
        } else {
            verdict =
                "The probability of " + names.get(0) + " swings between "
                    + Formatting.percent(min) + " and " + Formatting.percent(max)
                    + " as the phase turns once round. The classical mixture with the "
                    + "same Z-basis chances stays at " + Formatting.percent(mixed[0])
                    + " for every phase.";
        }

        return new Result(
            "P(not decayed) = " + Formatting.percent(p0)
                + ", P(decayed) = " + Formatting.percent(1.0 - p0)
                + "; |α| = " + Formatting.fixed(alpha, 4)
                + ", |β| = " + Formatting.fixed(beta, 4) + ".",
            "Measuring in the " + basisName(basis) + " basis. Plotted outcome: " + names.get(0)
                + "; the other outcome is its complement.",
            rows,
            Formatting.fixed(height, 4),
            Formatting.fixed(expected, 4),
            verdict,
            names.get(0),
            degrees(first),
            flat(mixed[0]));
    }

    private static ComplexMatrix diagonal(double p0) {
        return ComplexMatrix.of(
            new Complex[][] {
                {Complex.of(p0), Complex.of(0.0)},
                {Complex.of(0.0), Complex.of(1.0 - p0)}
            });
    }

    private static List<SeriesPoint> degrees(List<SeriesPoint> radians) {
        List<SeriesPoint> out = new ArrayList<>(radians.size());
        for (SeriesPoint p : radians) {
            out.add(new SeriesPoint(Math.toDegrees(p.x()), p.y()));
        }
        return out;
    }

    private static List<SeriesPoint> flat(double probability) {
        return List.of(new SeriesPoint(-180.0, probability), new SeriesPoint(180.0, probability));
    }

    private static List<String> outcomeNames(StandardBasis basis) {
        return switch (basis) {
            case COMPUTATIONAL -> List.of("not decayed |0⟩", "decayed |1⟩");
            case HADAMARD -> List.of("|+⟩", "|−⟩");
            case CIRCULAR -> List.of("|+i⟩", "|−i⟩");
        };
    }

    private static String basisName(StandardBasis basis) {
        return switch (basis) {
            case COMPUTATIONAL -> "computational (Z)";
            case HADAMARD -> "Hadamard (X)";
            case CIRCULAR -> "circular (Y)";
        };
    }
}
