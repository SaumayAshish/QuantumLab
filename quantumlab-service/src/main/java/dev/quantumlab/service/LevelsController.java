package dev.quantumlab.service;

import dev.quantumlab.physics.schrodinger.Grid1D;
import dev.quantumlab.physics.schrodinger.Hamiltonian1D;
import dev.quantumlab.physics.schrodinger.InfiniteWell;
import dev.quantumlab.physics.schrodinger.Potential;
import dev.quantumlab.simulation.ExplanationCatalog;
import dev.quantumlab.simulation.ModeExplanation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Energy levels of one particle in one dimension (infinite well or harmonic oscillator in a box),
 * by finite differences, next to the reference values.
 *
 * <p>The limits below are the same as on the Quantum well screen ({@code SchrodingerPresenter} in
 * the desktop module); that class cannot be used from here, so keep the two in step.
 */
@RestController
@RequestMapping("/api")
public class LevelsController {

    static final int MIN_POINTS = 10;
    static final int MAX_POINTS = 20_000;
    static final int MAX_LEVELS = 20;
    static final double MAX_WALL = 1_000_000.0;

    private enum Kind {
        WELL,
        HARMONIC
    }

    /**
     * One level. {@code n} follows the physics convention (the well starts at 1, the oscillator at
     * 0). {@code error} is numerical minus reference, and {@code relativeError} is that divided by
     * the reference, as a fraction (not a percentage).
     */
    public record Level(
        int n,
        double numerical,
        double reference,
        double error,
        double relativeError) {}

    /** The JSON body of {@code GET /api/levels}. Units are ħ = m = 1. */
    public record LevelsResult(
        String potential,
        String units,
        double xMin,
        double xMax,
        int gridPoints,
        double spacing,
        Double omega,
        String referenceMeaning,
        List<Level> levels,
        ModeExplanation explanation) {}

    @GetMapping("/levels")
    public LevelsResult levels(
        @RequestParam(name = "potential", required = false) String potential,
        @RequestParam(name = "xMin", required = false) String xMin,
        @RequestParam(name = "xMax", required = false) String xMax,
        @RequestParam(name = "points", required = false) String points,
        @RequestParam(name = "levels", required = false) String levels,
        @RequestParam(name = "omega", required = false) String omega) {
        Kind kind = parseKind(potential);
        double left = Numbers.parse(xMin, "xMin");
        double right = Numbers.parse(xMax, "xMax");
        if (Math.abs(left) > MAX_WALL || Math.abs(right) > MAX_WALL) {
            throw new BadRequestException(
                "xMin and xMax must lie within +-" + (long) MAX_WALL);
        }
        if (!(left < right)) {
            throw new BadRequestException("xMax must be greater than xMin");
        }
        int gridPoints = wholeInRange(points, "points", MIN_POINTS, MAX_POINTS);
        int count = wholeInRange(levels, "levels", 1, Math.min(MAX_LEVELS, gridPoints));

        double frequency = 1.0;
        Double frequencyOut = null;
        if (kind == Kind.HARMONIC) {
            frequency = Numbers.parse(omega, "omega");
            if (frequency <= 0.0 || frequency > MAX_WALL) {
                throw new BadRequestException(
                    "omega must be positive and at most " + (long) MAX_WALL + ", got " + omega);
            }
            frequencyOut = frequency;
        }

        try {
            Grid1D grid = new Grid1D(left, right, gridPoints);
            Potential v = kind == Kind.HARMONIC ? Potential.harmonic(frequency) : Potential.zero();
            double[] energies = new Hamiltonian1D(grid, v).energies(count);
            InfiniteWell well = kind == Kind.WELL ? new InfiniteWell(left, right) : null;

            List<Level> rows = new ArrayList<>(count);
            for (int k = 0; k < count; k++) {
                double reference = well != null ? well.energy(k + 1) : frequency * (k + 0.5);
                double error = energies[k] - reference;
                rows.add(
                    new Level(
                        well != null ? k + 1 : k,
                        energies[k],
                        reference,
                        error,
                        error / reference));
            }
            return new LevelsResult(
                kind.name().toLowerCase(Locale.ROOT),
                "ħ = m = 1",
                left,
                right,
                gridPoints,
                grid.spacing(),
                frequencyOut,
                referenceMeaning(kind),
                List.copyOf(rows),
                ExplanationCatalog.schrodinger1d());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private static String referenceMeaning(Kind kind) {
        return switch (kind) {
            case WELL -> "Exact energies of the infinite square well, E_n = n²π²/(2L²), n = 1, 2, ...";
            case HARMONIC ->
                "Energies ω(n + ½), n = 0, 1, ..., of the oscillator without walls. The box is"
                    + " finite, so levels whose classical turning point lies beyond a wall"
                    + " are raised.";
        };
    }

    private static Kind parseKind(String text) {
        if (text == null) {
            throw new BadRequestException("potential is required");
        }
        return switch (text.trim().toLowerCase(Locale.ROOT)) {
            case "well" -> Kind.WELL;
            case "harmonic" -> Kind.HARMONIC;
            default ->
                throw new BadRequestException(
                    "potential must be 'well' or 'harmonic', got '" + text + "'");
        };
    }

    private static int wholeInRange(String text, String field, int min, int max) {
        long value = Numbers.parseLong(text, field);
        if (value < min || value > max) {
            throw new BadRequestException(
                field + " must be between " + min + " and " + max + ", got " + text);
        }
        return (int) value;
    }
}
