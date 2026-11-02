package dev.quantumlab.desktop;

import dev.quantumlab.physics.dynamics.CrankNicolson;
import dev.quantumlab.physics.dynamics.GaussianPacket;
import dev.quantumlab.physics.dynamics.WaveFunction1D;
import dev.quantumlab.physics.scattering.PacketTransmission;
import dev.quantumlab.physics.scattering.RectangularBarrier;
import dev.quantumlab.physics.schrodinger.Grid1D;
import dev.quantumlab.physics.schrodinger.Potential;
import dev.quantumlab.simulation.Formatting;
import dev.quantumlab.simulation.SeriesPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * The logic behind the wave-packet screen, free of any JavaFX type. It validates the typed settings,
 * runs the time-dependent Schrödinger equation (ħ = m = 1) for a Gaussian packet that starts to the
 * left of a rectangular barrier, stores {@link #FRAMES} snapshots for the view to play back, and
 * compares the final split with the stationary prediction. Stateless, so safe from any thread; the
 * calculation takes a fraction of a second to a few seconds, so call it off the UI thread.
 *
 * <p>The numerics are fixed: walls at ±{@value #HALF_WIDTH}, spacing {@value #SPACING}, the barrier
 * starting at x = 0. The barrier width is rounded to a whole number of grid cells, and the setup text
 * says so, because the transmitted fraction is sensitive to the width.
 */
public final class WavePacketPresenter {

    public static final int FRAMES = 181;
    public static final double HALF_WIDTH = 80.0;
    public static final double SPACING = 0.1;
    /**
     * Every second grid point is kept for display.
     */
    public static final int DISPLAY_STRIDE = 2;

    private static final int GRID_POINTS = (int) Math.round(2.0 * HALF_WIDTH / SPACING) - 1;
    private static final double EDGE = 1e-9;
    private static final int MAX_STEPS = 100_000;

    /**
     * Raw field contents.
     */
    public record Request(String height, String width, String centre, String sigma,
                          String waveNumber) {
    }

    /**
     * One stored snapshot. The array holds the densities at the displayed positions only.
     */
    public record Frame(double time, double[] density, double left, double inside, double right) {
    }

    /**
     * The numbers shown beside the animation for one frame, already formatted.
     */
    public record FrameText(String time, String left, String inside, String right, String total) {
    }

    /**
     * @param positions      the displayed x values (shared by every frame)
     * @param barrierOutline the barrier drawn at the height of the initial density peak
     * @param leftOverTime   / insideOverTime / rightOverTime the probability in each region against time
     * @param theoryLine     the stationary prediction as a horizontal line over the whole run
     */
    public record Result(
        String setup,
        double[] positions,
        List<Frame> frames,
        List<SeriesPoint> barrierOutline,
        double maxDensity,
        double endTime,
        List<SeriesPoint> leftOverTime,
        List<SeriesPoint> insideOverTime,
        List<SeriesPoint> rightOverTime,
        List<SeriesPoint> theoryLine,
        String theoryAverage,
        String theoryAtCentralEnergy,
        String finalRight,
        String finalLeft,
        String finalInside,
        String normCheck,
        String summary,
        String note) {
    }

    public Result compute(Request r) {
        if (r == null) {
            throw new InvalidInputException("No settings were given.");
        }
        double height = InputParser.number(r.height(), "Barrier height V0");
        if (!(height > 0.0) || height > 50.0) {
            throw new InvalidInputException("Barrier height V0 must be greater than 0 and at most 50.");
        }
        double widthTyped = InputParser.number(r.width(), "Barrier width a");
        if (widthTyped < SPACING - 1e-12 || widthTyped > 10.0) {
            throw new InvalidInputException("Barrier width a must be between 0.1 and 10.");
        }
        double centre = InputParser.number(r.centre(), "Packet centre x0");
        double sigma = InputParser.number(r.sigma(), "Packet width σ");
        if (sigma < 1.0 || sigma > 8.0) {
            throw new InvalidInputException("Packet width σ must be between 1 and 8.");
        }
        double k0 = InputParser.number(r.waveNumber(), "Wave number k0");
        if (k0 < 0.5 || k0 > 4.0) {
            throw new InvalidInputException("Wave number k0 must be between 0.5 and 4.");
        }
        if (k0 * sigma < PacketTransmission.MIN_WAVE_NUMBER_TIMES_WIDTH) {
            throw new InvalidInputException(
                "k0 × σ must be at least 2.5: a packet this wide in momentum would partly move"
                    + " away from the barrier. Increase k0 or σ.");
        }
        if (centre + 6.0 * sigma > 0.0) {
            throw new InvalidInputException(
                "The packet must start entirely to the left of the barrier: x0 + 6σ must not exceed"
                    + " 0 (here x0 must be at most " + Formatting.fixed(-6.0 * sigma, 2) + ").");
        }
        if (centre - 6.0 * sigma < -HALF_WIDTH) {
            throw new InvalidInputException(
                "The packet is too close to the left wall: x0 − 6σ must stay above −80.");
        }

        int cells = (int) Math.round(widthTyped / SPACING);
        double width = cells * SPACING;
        double barrierEnd = width - EDGE;
        Potential potential = x -> x > -EDGE && x < barrierEnd ? height : 0.0;

        double after = width + 6.0 * sigma + 15.0;
        double endTime = (-centre + after) / k0;
        double spreadAtEnd = Math.sqrt(sigma * sigma + Math.pow(endTime / (2.0 * sigma), 2));
        if (after + 4.0 * spreadAtEnd > HALF_WIDTH) {
            throw new InvalidInputException(
                "This packet would spread until it reaches the walls before the collision is over."
                    + " Use a larger k0, a narrower packet or a start closer to the barrier.");
        }

        double stepGuess = Math.min(0.02, 0.1 / (height + 0.5 * k0 * k0));
        int perFrame = Math.max(1, (int) Math.ceil(endTime / stepGuess / (FRAMES - 1)));
        if ((long) perFrame * (FRAMES - 1) > MAX_STEPS) {
            throw new InvalidInputException("These settings need too many time steps; reduce V0 or the run length.");
        }
        double dt = endTime / (perFrame * (double) (FRAMES - 1));

        Grid1D grid = new Grid1D(-HALF_WIDTH, HALF_WIDTH, GRID_POINTS);
        CrankNicolson propagator = new CrankNicolson(grid, potential, dt);
        WaveFunction1D state = GaussianPacket.create(grid, centre, sigma, k0);
        double meanEnergy = propagator.energy(state);

        int shown = (GRID_POINTS + DISPLAY_STRIDE - 1) / DISPLAY_STRIDE;
        double[] positions = new double[shown];
        for (int j = 0; j < shown; j++) {
            positions[j] = grid.x(j * DISPLAY_STRIDE);
        }

        List<Frame> frames = new ArrayList<>(FRAMES);
        List<SeriesPoint> left = new ArrayList<>(FRAMES);
        List<SeriesPoint> inside = new ArrayList<>(FRAMES);
        List<SeriesPoint> right = new ArrayList<>(FRAMES);
        double worstNormError = 0.0;
        double maxDensity = 0.0;
        for (int f = 0; f < FRAMES; f++) {
            if (f > 0) {
                state = propagator.evolve(state, perFrame);
            }
            double time = f * perFrame * dt;
            double[] density = new double[shown];
            for (int j = 0; j < shown; j++) {
                density[j] = state.density(j * DISPLAY_STRIDE);
                maxDensity = Math.max(maxDensity, density[j]);
            }
            double l = state.probabilityBetween(-1e9, -EDGE);
            double in = state.probabilityBetween(-EDGE, barrierEnd);
            double rt = state.probabilityBetween(barrierEnd, 1e9);
            worstNormError = Math.max(worstNormError, Math.abs(state.totalProbability() - 1.0));
            frames.add(new Frame(time, density, l, in, rt));
            left.add(new SeriesPoint(time, l));
            inside.add(new SeriesPoint(time, in));
            right.add(new SeriesPoint(time, rt));
        }

        RectangularBarrier barrier = new RectangularBarrier(height, width);
        double theory = PacketTransmission.average(barrier, sigma, k0);
        double atMean = barrier.transmission(0.5 * k0 * k0);
        Frame last = frames.get(FRAMES - 1);

        double scale = maxOf(frames.get(0).density());
        List<SeriesPoint> outline =
            List.of(
                new SeriesPoint(-HALF_WIDTH, 0.0),
                new SeriesPoint(-EDGE, 0.0),
                new SeriesPoint(0.0, scale),
                new SeriesPoint(width, scale),
                new SeriesPoint(width + EDGE, 0.0),
                new SeriesPoint(HALF_WIDTH, 0.0));
        List<SeriesPoint> theoryLine =
            List.of(new SeriesPoint(0.0, theory), new SeriesPoint(endTime, theory));

        String setup =
            "Barrier: height V0 = "
                + Formatting.fixed(height, 4)
                + ", width a = "
                + Formatting.fixed(width, 4)
                + (Math.abs(width - widthTyped) > 1e-9
                ? " (typed " + Formatting.fixed(widthTyped, 4) + ", rounded to " + cells
                + " grid cells of " + Formatting.fixed(SPACING, 1) + ")"
                : " (" + cells + " grid cells of " + Formatting.fixed(SPACING, 1) + ")")
                + ". Packet: centre x0 = "
                + Formatting.fixed(centre, 4)
                + ", width σ = "
                + Formatting.fixed(sigma, 4)
                + ", wave number k0 = "
                + Formatting.fixed(k0, 4)
                + ", mean energy ⟨E⟩ = "
                + Formatting.fixed(meanEnergy, 4)
                + ". Run time "
                + Formatting.fixed(endTime, 2)
                + ", time step "
                + Formatting.fixed(dt, 4)
                + ".";
        String summary =
            "At the end, the probability of finding the particle beyond the barrier is "
                + TunnellingPresenter.probability(last.right())
                + " and before it "
                + TunnellingPresenter.probability(last.left())
                + ". Stationary theory, averaging T(E) over the packet's spread of energies,"
                + " predicts "
                + TunnellingPresenter.probability(theory)
                + "; T at the central energy k0²/2 alone would give "
                + TunnellingPresenter.probability(atMean)
                + ". The two differ because a packet is a mixture of energies and T depends on"
                + " energy.";
        String note =
            "This is the Schrödinger equation solved numerically for a single particle: the"
                + " probability density splits into a reflected and a transmitted part. It does"
                + " not describe the cat or any measurement; a measurement would find the particle"
                + " on one side, with these probabilities. Results depend on the grid (spacing "
                + Formatting.fixed(SPACING, 1)
                + ") and on the barrier width being a whole number of cells.";
        return new Result(
            setup,
            positions,
            List.copyOf(frames),
            outline,
            maxDensity,
            endTime,
            List.copyOf(left),
            List.copyOf(inside),
            List.copyOf(right),
            theoryLine,
            TunnellingPresenter.probability(theory),
            TunnellingPresenter.probability(atMean),
            TunnellingPresenter.probability(last.right()),
            TunnellingPresenter.probability(last.left()),
            TunnellingPresenter.probability(last.inside()),
            ConvergencePresenter.scientific(worstNormError == 0.0 ? 1e-16 : worstNormError),
            summary,
            note);
    }

    /**
     * The formatted numbers for one frame.
     */
    public static FrameText describe(Frame frame) {
        return new FrameText(
            Formatting.fixed(frame.time(), 2),
            TunnellingPresenter.probability(frame.left()),
            TunnellingPresenter.probability(frame.inside()),
            TunnellingPresenter.probability(frame.right()),
            Formatting.fixed(frame.left() + frame.inside() + frame.right(), 6));
    }

    private static double maxOf(double[] values) {
        double m = 0.0;
        for (double v : values) {
            m = Math.max(m, v);
        }
        return m;
    }
}
