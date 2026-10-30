package dev.quantumlab.desktop;

import dev.quantumlab.physics.schrodinger.Grid1D;
import dev.quantumlab.physics.schrodinger.Hamiltonian1D;
import dev.quantumlab.physics.schrodinger.InfiniteWell;
import dev.quantumlab.physics.schrodinger.Potential;
import dev.quantumlab.physics.schrodinger.StationaryState;
import dev.quantumlab.simulation.Formatting;
import dev.quantumlab.simulation.SeriesPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * The logic behind the one-dimensional Schrödinger screen, free of any JavaFX type.
 *
 * <p>It validates the typed settings, solves the discretised problem, and returns immutable,
 * already-formatted views: a table comparing each numerical energy with its analytic reference, and
 * the wavefunction of a chosen level as plot series. Units are ħ = m = 1. Not thread-safe: confine
 * to one worker thread, as for the other presenters.
 */
public final class SchrodingerPresenter {

    /** The potentials offered. */
    public enum PotentialKind {
        INFINITE_WELL,
        HARMONIC
    }

    public static final int MIN_POINTS = 10;
    public static final int MAX_POINTS = 20_000;
    public static final int MAX_LEVELS = 20;
    static final int MAX_PLOT_POINTS = 400;
    private static final double MAX_WALL = 1_000_000.0;

    /** Raw field contents. {@code omega} is only read for the harmonic oscillator. */
    public record Request(
        PotentialKind potential,
        String xMin,
        String xMax,
        String gridPoints,
        String omega,
        String levels) {}

    /**
     * What was solved.
     *
     * @param note things to keep in mind when reading the numbers; may be empty
     */
    public record SetupView(
        String potential, String domain, String gridPoints, String spacing, String note) {}

    /** One row of the energy table. */
    public record LevelRow(
        String label, String numerical, String reference, String error, String relativeError) {}

    public record SolutionView(SetupView setup, List<LevelRow> levels) {}

    /**
     * One wavefunction as plot series. {@code exact} is empty when no analytic wavefunction is known.
     * {@code potential} holds {@code V(x)} at the plotted interior points.
     */
    public record WaveView(
        String label,
        String energy,
        int nodes,
        String normalisation,
        List<SeriesPoint> amplitude,
        List<SeriesPoint> density,
        List<SeriesPoint> potential,
        List<SeriesPoint> exact) {}

    private record Solved(
        PotentialKind kind,
        double omega,
        Grid1D grid,
        Potential potential,
        Hamiltonian1D hamiltonian,
        int levels) {}

    private Solved current;

    public boolean hasSolution() {
        return current != null;
    }

    /**
     * Validates the request, solves it, and replaces any previous solution.
     *
     * @throws InvalidInputException if a field is invalid; the previous solution is kept
     */
    public SolutionView solve(Request r) {
        if (r == null || r.potential() == null) {
            throw new InvalidInputException("No settings were given.");
        }
        double xMin = InputParser.number(r.xMin(), "Left wall");
        double xMax = InputParser.number(r.xMax(), "Right wall");
        if (Math.abs(xMin) > MAX_WALL || Math.abs(xMax) > MAX_WALL) {
            throw new InvalidInputException("The walls must lie within ±" + (long) MAX_WALL + ".");
        }
        if (!(xMin < xMax)) {
            throw new InvalidInputException("The right wall must be to the right of the left wall.");
        }
        int points = wholeNumber(r.gridPoints(), "Grid points", MIN_POINTS, MAX_POINTS);
        int levels = wholeNumber(r.levels(), "Number of levels", 1, Math.min(MAX_LEVELS, points));
        double omega = 1.0;
        if (r.potential() == PotentialKind.HARMONIC) {
            omega = InputParser.number(r.omega(), "Oscillator frequency ω");
            if (omega <= 0.0 || omega > MAX_WALL) {
                throw new InvalidInputException("Oscillator frequency ω must be positive.");
            }
        }

        Grid1D grid = new Grid1D(xMin, xMax, points);
        Potential potential =
            r.potential() == PotentialKind.HARMONIC ? Potential.harmonic(omega) : Potential.zero();
        Hamiltonian1D hamiltonian = new Hamiltonian1D(grid, potential);
        double[] energies = hamiltonian.energies(levels);

        current = new Solved(r.potential(), omega, grid, potential, hamiltonian, levels);

        List<LevelRow> rows = new ArrayList<>(levels);
        for (int k = 0; k < levels; k++) {
            double reference = reference(current, k);
            double error = energies[k] - reference;
            rows.add(
                new LevelRow(
                    label(current, k),
                    Formatting.fixed(energies[k], 6),
                    Formatting.fixed(reference, 6),
                    Formatting.fixed(error, 6),
                    Formatting.fixed(100.0 * error / reference, 4) + " %"));
        }
        return new SolutionView(setup(current), rows);
    }

    /**
     * The wavefunction of the level at table position {@code index} (0 is the lowest).
     *
     * @throws InvalidInputException if the index is not one of the solved levels
     */
    public WaveView wave(int index) {
        Solved s = requireSolution();
        if (index < 0 || index >= s.levels()) {
            throw new InvalidInputException(
                "Level index must be between 0 and " + (s.levels() - 1) + ", got " + index + ".");
        }
        StationaryState state = s.hamiltonian().state(index);
        Grid1D grid = s.grid();
        int n = grid.interiorPoints();
        int stride = (n + MAX_PLOT_POINTS - 1) / MAX_PLOT_POINTS;
        InfiniteWell well =
            s.kind() == PotentialKind.INFINITE_WELL
                ? new InfiniteWell(grid.xMin(), grid.xMax())
                : null;

        List<SeriesPoint> amplitude = new ArrayList<>();
        List<SeriesPoint> density = new ArrayList<>();
        List<SeriesPoint> potential = new ArrayList<>();
        List<SeriesPoint> exact = new ArrayList<>();

        amplitude.add(new SeriesPoint(grid.xMin(), 0.0));
        density.add(new SeriesPoint(grid.xMin(), 0.0));
        if (well != null) {
            exact.add(new SeriesPoint(grid.xMin(), 0.0));
        }
        for (int i = 0; i < n; i += stride) {
            addSample(s, state, well, index, i, amplitude, density, potential, exact);
        }
        if ((n - 1) % stride != 0) {
            addSample(s, state, well, index, n - 1, amplitude, density, potential, exact);
        }
        amplitude.add(new SeriesPoint(grid.xMax(), 0.0));
        density.add(new SeriesPoint(grid.xMax(), 0.0));
        if (well != null) {
            exact.add(new SeriesPoint(grid.xMax(), 0.0));
        }

        return new WaveView(
            label(s, index),
            Formatting.fixed(state.energy(), 6),
            state.nodes(),
            Formatting.fixed(state.probability(0, n), 6),
            List.copyOf(amplitude),
            List.copyOf(density),
            List.copyOf(potential),
            List.copyOf(exact));
    }

    private static void addSample(
        Solved s,
        StationaryState state,
        InfiniteWell well,
        int index,
        int i,
        List<SeriesPoint> amplitude,
        List<SeriesPoint> density,
        List<SeriesPoint> potential,
        List<SeriesPoint> exact) {
        double x = s.grid().x(i);
        amplitude.add(new SeriesPoint(x, state.amplitude(i)));
        density.add(new SeriesPoint(x, state.density(i)));
        potential.add(new SeriesPoint(x, s.potential().value(x)));
        if (well != null) {
            exact.add(new SeriesPoint(x, well.wavefunction(index + 1, x)));
        }
    }

    // ---------------------------------------------------------------- helpers

    private static double reference(Solved s, int index) {
        return switch (s.kind()) {
            case INFINITE_WELL -> new InfiniteWell(s.grid().xMin(), s.grid().xMax()).energy(index + 1);
            case HARMONIC -> s.omega() * (index + 0.5);
        };
    }

    /** Physics convention: the well's levels start at n = 1, the oscillator's at n = 0. */
    private static String label(Solved s, int index) {
        return "n = " + (s.kind() == PotentialKind.INFINITE_WELL ? index + 1 : index);
    }

    private static SetupView setup(Solved s) {
        Grid1D g = s.grid();
        String potential =
            switch (s.kind()) {
                case INFINITE_WELL -> "Infinite square well: V = 0 between hard walls";
                case HARMONIC ->
                    "Harmonic oscillator: V = ½ω²x² with ω = "
                        + Formatting.fixed(s.omega(), 4)
                        + ", hard walls at the box edges";
            };
        String note =
            switch (s.kind()) {
                case INFINITE_WELL ->
                    "Numerical levels lie below the exact ones; the gap grows with n and"
                        + " shrinks like h².";
                case HARMONIC -> harmonicNote(s);
            };
        return new SetupView(
            potential,
            "[" + Formatting.fixed(g.xMin(), 4) + ", " + Formatting.fixed(g.xMax(), 4) + "]",
            Integer.toString(g.interiorPoints()),
            Formatting.fixed(g.spacing(), 6),
            note);
    }

    private static String harmonicNote(Solved s) {
        Grid1D g = s.grid();
        String base = "Reference values ω(n + ½) assume no walls; the box is finite.";
        if (!(g.xMin() < 0.0 && g.xMax() > 0.0)) {
            return base + " The box does not contain the potential minimum at x = 0.";
        }
        double reach = Math.min(-g.xMin(), g.xMax());
        for (int k = 0; k < s.levels(); k++) {
            double turningPoint = Math.sqrt(2.0 * reference(s, k)) / s.omega();
            if (turningPoint > reach) {
                return base
                    + " From n = "
                    + k
                    + " upward the classical turning point lies beyond the nearer wall, so the"
                    + " walls raise those energies.";
            }
        }
        return base;
    }

    private static int wholeNumber(String text, String field, int min, int max) {
        double v = InputParser.number(text, field);
        if (v != Math.rint(v)) {
            throw new InvalidInputException(field + " must be a whole number.");
        }
        if (v < min || v > max) {
            throw new InvalidInputException(field + " must be between " + min + " and " + max + ".");
        }
        return (int) v;
    }

    private Solved requireSolution() {
        if (current == null) {
            throw new IllegalStateException("Nothing has been solved yet.");
        }
        return current;
    }
}
