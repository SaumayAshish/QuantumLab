package dev.quantumlab.desktop;

import dev.quantumlab.physics.schrodinger.ConvergenceStudy;
import dev.quantumlab.physics.schrodinger.InfiniteWell;
import dev.quantumlab.physics.schrodinger.Potential;
import dev.quantumlab.simulation.Formatting;
import dev.quantumlab.simulation.SeriesPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The logic behind the convergence screen, free of any JavaFX type: it validates the typed
 * settings, refines the grid step by step, and returns pre-formatted rows and plot series.
 *
 * <p>The error is plotted as {@code log10 |error|} against {@code log10 h}, so a second-order scheme
 * lies on a straight line of slope 2. Stateless, so it may be used from any thread.
 */
public final class ConvergencePresenter {

    public static final int MIN_REFINEMENTS = 2;
    public static final int MAX_REFINEMENTS = 10;

    /** Raw field contents. {@code omega} is only read for the harmonic oscillator. */
    public record Request(
        SchrodingerPresenter.PotentialKind potential,
        String xMin,
        String xMax,
        String omega,
        String level,
        String firstGrid,
        String steps) {}

    /** One grid size, already formatted. */
    public record RowView(
        String points, String spacing, String numerical, String error, String order) {}

    /**
     * @param errors points {@code (log10 h, log10 |error|)}; rows with an exactly zero error are left out
     * @param guide two points on a line of slope 2 through the first error point, empty if fewer than
     *     two error points exist
     */
    public record Result(
        String title,
        String reference,
        String note,
        List<RowView> rows,
        List<SeriesPoint> errors,
        List<SeriesPoint> guide,
        String summary) {}

    private static final double MAX_WALL = 1_000_000.0;

    public Result study(Request r) {
        if (r == null || r.potential() == null) {
            throw new InvalidInputException("No settings were given.");
        }
        boolean well = r.potential() == SchrodingerPresenter.PotentialKind.INFINITE_WELL;
        double xMin = InputParser.number(r.xMin(), "Left wall");
        double xMax = InputParser.number(r.xMax(), "Right wall");
        if (Math.abs(xMin) > MAX_WALL || Math.abs(xMax) > MAX_WALL) {
            throw new InvalidInputException("The walls must lie within ±" + (long) MAX_WALL + ".");
        }
        if (!(xMin < xMax)) {
            throw new InvalidInputException("The right wall must be to the right of the left wall.");
        }
        double omega = 1.0;
        if (!well) {
            omega = InputParser.number(r.omega(), "Oscillator frequency ω");
            if (omega <= 0.0 || omega > MAX_WALL) {
                throw new InvalidInputException("Oscillator frequency ω must be positive.");
            }
        }
        int lowest = well ? 1 : 0;
        int highest = well ? SchrodingerPresenter.MAX_LEVELS : SchrodingerPresenter.MAX_LEVELS - 1;
        int n = SchrodingerPresenter.wholeNumber(r.level(), "Level n", lowest, highest);
        int index = well ? n - 1 : n;
        int first =
            SchrodingerPresenter.wholeNumber(
                r.firstGrid(),
                "First grid size",
                SchrodingerPresenter.MIN_POINTS,
                SchrodingerPresenter.MAX_POINTS);
        if (index >= first) {
            throw new InvalidInputException("The first grid must have more points than the level index.");
        }
        int steps =
            SchrodingerPresenter.wholeNumber(
                r.steps(), "Number of refinements", MIN_REFINEMENTS, MAX_REFINEMENTS);
        long finest = ((first + 1L) << (steps - 1)) - 1;
        if (finest > SchrodingerPresenter.MAX_POINTS) {
            throw new InvalidInputException(
                "The finest grid would have "
                    + finest
                    + " points; the limit is "
                    + SchrodingerPresenter.MAX_POINTS
                    + ". Use a smaller first grid or fewer refinements.");
        }

        double reference =
            well ? new InfiniteWell(xMin, xMax).energy(n) : omega * (n + 0.5);
        Potential potential = well ? Potential.zero() : Potential.harmonic(omega);
        List<ConvergenceStudy.Row> rows =
            ConvergenceStudy.run(
                potential,
                xMin,
                xMax,
                index,
                reference,
                ConvergenceStudy.halvingGrids(first, steps));

        List<RowView> views = new ArrayList<>(rows.size());
        List<SeriesPoint> errors = new ArrayList<>();
        double lastOrder = Double.NaN;
        for (ConvergenceStudy.Row row : rows) {
            views.add(
                new RowView(
                    Integer.toString(row.interiorPoints()),
                    Formatting.fixed(row.spacing(), 6),
                    Formatting.fixed(row.numerical(), 8),
                    scientific(row.error()),
                    Double.isNaN(row.observedOrder())
                        ? "–"
                        : Formatting.fixed(row.observedOrder(), 4)));
            if (row.error() != 0.0) {
                errors.add(
                    new SeriesPoint(
                        Math.log10(row.spacing()), Math.log10(Math.abs(row.error()))));
            }
            if (!Double.isNaN(row.observedOrder())) {
                lastOrder = row.observedOrder();
            }
        }

        List<SeriesPoint> guide = List.of();
        if (errors.size() >= 2) {
            SeriesPoint a = errors.get(0);
            SeriesPoint b = errors.get(errors.size() - 1);
            guide = List.of(a, new SeriesPoint(b.x(), a.y() + 2.0 * (b.x() - a.x())));
        }

        String summary =
            Double.isNaN(lastOrder)
                ? "No observed order: an error was exactly zero."
                : "Observed order at the finest refinement: "
                + Formatting.fixed(lastOrder, 4)
                + ". The three-point scheme is second order, so 2 is expected.";
        if (!well && !Double.isNaN(lastOrder) && lastOrder < 1.5) {
            summary += " That is well below 2: the fixed wall error now dominates the grid error.";
        }
        String note =
            well
                ? "The reference energy is exact, so the error is pure discretisation error"
                + " and should fall like h²."
                : "The walls add a fixed error that does not shrink with h: in a small box the"
                + " observed order drops below 2 once that error dominates.";
        String title =
            (well ? "Infinite square well" : "Harmonic oscillator (ω = " + Formatting.fixed(omega, 4) + ")")
                + ", n = "
                + n;
        return new Result(
            title,
            Formatting.fixed(reference, 6),
            note,
            List.copyOf(views),
            List.copyOf(errors),
            guide,
            summary);
    }

    /** Scientific notation with a true minus sign, e.g. {@code −1.583e-02}. */
    static String scientific(double value) {
        String s = String.format(Locale.ROOT, "%.3e", value);
        return s.startsWith("-") ? "−" + s.substring(1) : s;
    }
}
