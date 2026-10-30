package dev.quantumlab.desktop;

import dev.quantumlab.physics.scattering.Layer;
import dev.quantumlab.physics.scattering.RectangularBarrier;
import dev.quantumlab.physics.scattering.Scattering;
import dev.quantumlab.simulation.Formatting;
import dev.quantumlab.simulation.SeriesPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * The logic behind the tunnelling screen, free of any JavaFX type: it validates the typed settings,
 * computes the transmission probability T(E) of one or two rectangular barriers (ħ = m = 1), and
 * returns pre-formatted views. Stateless, so it may be used from any thread.
 *
 * <p>Everything here is stationary: T is the probability for a wave of one definite energy, not the
 * motion of a wave packet.
 */
public final class TunnellingPresenter {

    /** The barrier arrangements offered. */
    public enum BarrierKind {
        SINGLE,
        DOUBLE
    }

    public static final int CURVE_POINTS = 400;

    private static final double MAX_HEIGHT = 1_000.0;
    private static final double MAX_LENGTH = 100.0;
    private static final double MAX_ENERGY = 10_000.0;
    private static final double[] FRACTIONS = {0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0};

    /** Raw field contents. {@code gap} is only read for the double barrier. */
    public record Request(
        BarrierKind kind,
        String height,
        String width,
        String gap,
        String maxEnergy,
        String probeEnergy) {}

    /** One row of the comparison table, at an energy that is a fixed fraction of the height. */
    public record RowView(
        String fraction,
        String energy,
        String regime,
        String classical,
        String transmission,
        String reflection,
        String closedForm) {}

    /** The result at the energy the user asked about. */
    public record ProbeView(
        String energy,
        String regime,
        String classical,
        String transmission,
        String reflection,
        String decayConstant,
        String thickEstimate) {}

    /**
     * @param curve T against E on {@link #CURVE_POINTS} energies up to the maximum
     * @param barrierTop a vertical line at E = V0, empty if V0 is beyond the plotted range
     * @param probe a vertical line from 0 up to T at the probe energy, empty if beyond the range
     * @param note what to look for in this arrangement
     */
    public record Result(
        String setup,
        List<SeriesPoint> curve,
        List<SeriesPoint> barrierTop,
        List<SeriesPoint> probe,
        ProbeView probeView,
        List<RowView> rows,
        String peak,
        String note) {}

    public Result compute(Request r) {
        if (r == null || r.kind() == null) {
            throw new InvalidInputException("No settings were given.");
        }
        boolean single = r.kind() == BarrierKind.SINGLE;
        double height = bounded(r.height(), "Barrier height V0", MAX_HEIGHT);
        double width = bounded(r.width(), "Barrier width a", MAX_LENGTH);
        double gap = single ? 0.0 : bounded(r.gap(), "Gap between the barriers", MAX_LENGTH);
        double maxEnergy = bounded(r.maxEnergy(), "Maximum energy", MAX_ENERGY);
        double probeEnergy = bounded(r.probeEnergy(), "Probe energy E", MAX_ENERGY);

        List<Layer> layers =
            single
                ? List.of(new Layer(height, width))
                : List.of(new Layer(height, width), new Layer(0.0, gap), new Layer(height, width));
        RectangularBarrier closed = single ? new RectangularBarrier(height, width) : null;

        List<SeriesPoint> curve = new ArrayList<>(CURVE_POINTS);
        double bestT = -1.0;
        double bestE = 0.0;
        for (int i = 0; i < CURVE_POINTS; i++) {
            double e = maxEnergy * (i + 1) / CURVE_POINTS;
            double t = Scattering.transmission(layers, e);
            curve.add(new SeriesPoint(e, t));
            if (e < height && t > bestT) {
                bestT = t;
                bestE = e;
            }
        }

        List<SeriesPoint> top =
            height <= maxEnergy
                ? List.of(new SeriesPoint(height, 0.0), new SeriesPoint(height, 1.0))
                : List.of();
        double probeT = Scattering.transmission(layers, probeEnergy);
        List<SeriesPoint> probe =
            probeEnergy <= maxEnergy
                ? List.of(new SeriesPoint(probeEnergy, 0.0), new SeriesPoint(probeEnergy, probeT))
                : List.of();

        List<RowView> rows = new ArrayList<>();
        for (double f : FRACTIONS) {
            double e = f * height;
            double t = Scattering.transmission(layers, e);
            rows.add(
                new RowView(
                    "E = " + Formatting.fixed(f, 2) + " V0",
                    Formatting.fixed(e, 4),
                    regime(e, height),
                    classical(e, height),
                    probability(t),
                    probability(1.0 - t),
                    closed == null ? "–" : probability(closed.transmission(e))));
        }

        boolean below = probeEnergy < height;
        ProbeView probeView =
            new ProbeView(
                Formatting.fixed(probeEnergy, 4),
                regime(probeEnergy, height),
                classical(probeEnergy, height),
                probability(probeT),
                probability(1.0 - probeT),
                below ? Formatting.fixed(Math.sqrt(2.0 * (height - probeEnergy)), 4) : "–",
                below && closed != null ? probability(closed.thickBarrierEstimate(probeEnergy)) : "–");

        String setup =
            single
                ? "Single barrier: height V0 = "
                + Formatting.fixed(height, 4)
                + ", width a = "
                + Formatting.fixed(width, 4)
                : "Double barrier: two barriers of height V0 = "
                + Formatting.fixed(height, 4)
                + " and width a = "
                + Formatting.fixed(width, 4)
                + ", separated by a gap of "
                + Formatting.fixed(gap, 4);
        String peak =
            single
                ? ""
                : bestT < 0.0
                ? "No plotted energy lies below the barrier top."
                : "Highest transmission below the barrier top: T = "
                + probability(bestT)
                + " at E ≈ "
                + Formatting.fixed(bestE, 4)
                + " (resolution "
                + Formatting.fixed(maxEnergy / CURVE_POINTS, 4)
                + ").";
        String note =
            single
                ? "Below V0 a classical particle is always reflected (T = 0) but T is above 0:"
                + " tunnelling. Above V0 a classical particle always passes (T = 1) but T"
                + " is below 1 except at isolated energies where ka is a multiple of π."
                : "Each barrier alone transmits poorly below V0, yet the pair transmits strongly"
                + " at energies where the wave fits between them: resonant tunnelling.";
        return new Result(
            setup,
            List.copyOf(curve),
            top,
            probe,
            probeView,
            List.copyOf(rows),
            peak,
            note);
    }

    // ---------------------------------------------------------------- helpers

    private static double bounded(String text, String field, double max) {
        double v = InputParser.number(text, field);
        if (!(v > 0.0) || v > max) {
            throw new InvalidInputException(
                field + " must be greater than 0 and at most " + Formatting.fixed(max, 0) + ".");
        }
        return v;
    }

    private static String regime(double energy, double height) {
        if (energy < height) {
            return "below the barrier";
        }
        return energy > height ? "above the barrier" : "at the top";
    }

    private static String classical(double energy, double height) {
        if (energy < height) {
            return "0";
        }
        return energy > height ? "1" : "–";
    }

    /** Fixed decimals, or scientific notation when the value is small but not zero. */
    static String probability(double p) {
        if (p == 0.0) {
            return "0";
        }
        if (Math.abs(p) < 1e-4) {
            return ConvergencePresenter.scientific(p);
        }
        return Formatting.fixed(p, 6);
    }
}
