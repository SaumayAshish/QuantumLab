package dev.quantumlab.desktop;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexMatrix;
import dev.quantumlab.physics.MeasurementBasis;
import dev.quantumlab.physics.QubitState;
import dev.quantumlab.physics.composite.Chsh;
import dev.quantumlab.physics.composite.ClassicalCorrelation;
import dev.quantumlab.physics.composite.TwoQubitState;
import dev.quantumlab.simulation.Formatting;
import dev.quantumlab.simulation.SeriesPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * The logic behind the composite-system screen, free of any JavaFX type. It builds the joint state
 * {@code α|00⟩ + β|11⟩} of a nucleus and an idealised detector from the typed probability and
 * phase, builds the classical mixture with the same chances, and returns pre-formatted views of
 * their joint measurement statistics, entanglement and CHSH value. Stateless, so it may be used
 * from any thread.
 *
 * <p>Every number is an exact probability computed from the model. Nothing is sampled, nothing
 * evolves in time, and the detector is a two-level stand-in.
 */
public final class CompositePresenter {

    /** The measurement bases offered for each qubit. */
    public enum BasisChoice {
        COMPUTATIONAL,
        HADAMARD,
        CIRCULAR;

        MeasurementBasis basis() {
            return switch (this) {
                case COMPUTATIONAL -> MeasurementBasis.computational();
                case HADAMARD -> MeasurementBasis.hadamard();
                case CIRCULAR -> MeasurementBasis.circular();
            };
        }
    }

    public static final int CURVE_POINTS = 181;

    private static final double MAX_PHASE = 1_000.0;
    private static final double NOISE = 1e-12;
    private static final double ENTANGLED_ABOVE = 1e-9;

    /** Raw field contents and the two chosen bases. */
    public record Request(
        String notDecayed, String phase, BasisChoice nucleusBasis, BasisChoice detectorBasis) {}

    /** One joint outcome: the chance for the entangled state, for the mixture, and their difference. */
    public record RowView(String outcome, String entangled, String mixture, String difference) {}

    /**
     * @param entangledCurve the correlation E when both qubits are measured along matching axes at
     *     polar angle θ (degrees, 0 to 180), for the entangled state
     * @param mixtureCurve the same for the classical mixture
     */
    public record Result(
        String setup,
        String bases,
        List<RowView> rows,
        String agreeEntangled,
        String agreeMixture,
        String alone,
        String verdictBases,
        String concurrence,
        String entropy,
        String purity,
        String entanglementNote,
        String settings,
        String chshEntangled,
        String chshMixtureSame,
        String chshMixtureBest,
        String chshMaximum,
        String chshVerdict,
        List<SeriesPoint> entangledCurve,
        List<SeriesPoint> mixtureCurve) {}

    public Result compute(Request r) {
        if (r == null || r.nucleusBasis() == null || r.detectorBasis() == null) {
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

        double amplitude1 = Math.sqrt(1.0 - p0);
        QubitState nucleus =
            QubitState.of(
                Complex.of(Math.sqrt(p0), 0.0),
                Complex.of(amplitude1 * Math.cos(phi), amplitude1 * Math.sin(phi)));
        TwoQubitState entangled = TwoQubitState.correlated(nucleus);
        ClassicalCorrelation mixture = new ClassicalCorrelation(p0);

        MeasurementBasis nb = r.nucleusBasis().basis();
        MeasurementBasis db = r.detectorBasis().basis();
        double[][] pe = entangled.jointProbabilities(nb, db);
        double[][] pm = mixture.jointProbabilities(nb, db);

        List<RowView> rows = new ArrayList<>();
        double maxDifference = 0.0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                double diff = pe[i][j] - pm[i][j];
                maxDifference = Math.max(maxDifference, Math.abs(diff));
                rows.add(
                    new RowView(
                        "nucleus " + nb.label(i) + ", detector " + db.label(j),
                        probability(pe[i][j]),
                        probability(pm[i][j]),
                        Formatting.fixed(diff, 6)));
            }
        }

        String alone =
            "Each qubit on its own: nucleus "
                + nb.label(0)
                + " "
                + probability(pe[0][0] + pe[0][1])
                + " (entangled) and "
                + probability(pm[0][0] + pm[0][1])
                + " (mixture); detector "
                + db.label(0)
                + " "
                + probability(pe[0][0] + pe[1][0])
                + " (entangled) and "
                + probability(pm[0][0] + pm[1][0])
                + " (mixture).";
        String verdictBases =
            maxDifference < 1e-9
                ? "In these two bases the entangled state and the classical mixture give the same "
                + "statistics, so this choice cannot tell them apart."
                : "In these two bases the entangled state and the classical mixture give "
                + "different joint statistics.";

        double conc = entangled.concurrence();
        boolean isEntangled = conc > ENTANGLED_ABOVE;
        String entanglementNote =
            isEntangled
                ? "Entangled: neither qubit has a state of its own. Each alone is a mixture "
                + "with the purity shown, although the pair is in a pure joint state."
                : "Not entangled: the joint state is a product, so the nucleus and the "
                + "detector each have a state of their own.";

        // CHSH: nucleus axes Z and X; detector axes at ±atan(C) from z, turned about z by the phase.
        double t = Math.atan(conc);
        ComplexMatrix a = Chsh.observable(0.0);
        ComplexMatrix a2 = Chsh.observable(Math.PI / 2.0);
        ComplexMatrix b = Chsh.observable(t, phi);
        ComplexMatrix b2 = Chsh.observable(-t, phi);
        double sEntangled = Chsh.value(entangled::expectation, a, a2, b, b2);
        double sMixtureSame = Chsh.value(mixture::expectation, a, a2, b, b2);
        double sMixtureBest =
            Chsh.value(
                mixture::expectation, a, a2, Chsh.observable(0.0), Chsh.observable(0.0));
        double maximum = Chsh.maximumInPlane(conc);
        String settings =
            "Nucleus axes: z and x. Detector axes: "
                + Formatting.fixed(Math.toDegrees(t), 2)
                + "° and "
                + Formatting.fixed(-Math.toDegrees(t), 2)
                + "° from z, turned about z by the relative phase "
                + Formatting.fixed(Math.toDegrees(phi), 2)
                + "°.";
        boolean violates = sEntangled > Chsh.CLASSICAL_BOUND + 1e-9;
        String verdict =
            violates
                ? "S is above the classical bound 2. No assignment of outcomes fixed in "
                + "advance by a shared classical variable can give this for these two "
                + "qubits, and the classical mixture cannot exceed 2 with any settings."
                : "S does not exceed 2. The state is not entangled, so a shared classical "
                + "variable can reproduce its correlations.";

        List<SeriesPoint> curveE = new ArrayList<>(CURVE_POINTS);
        List<SeriesPoint> curveM = new ArrayList<>(CURVE_POINTS);
        for (int k = 0; k < CURVE_POINTS; k++) {
            double theta = Math.toRadians(k);
            ComplexMatrix na = Chsh.observable(theta);
            ComplexMatrix nd = Chsh.observable(theta, phi);
            curveE.add(new SeriesPoint(k, entangled.expectation(na, nd)));
            curveM.add(new SeriesPoint(k, mixture.expectation(na, nd)));
        }

        String setup =
            "Entangled state α|00⟩ + β|11⟩ with |α|² = "
                + Formatting.fixed(p0, 4)
                + ", |β|² = "
                + Formatting.fixed(1.0 - p0, 4)
                + ", relative phase φ = "
                + Formatting.fixed(phi, 4)
                + " rad. Classical mixture: |00⟩ with probability "
                + Formatting.fixed(p0, 4)
                + ", |11⟩ with probability "
                + Formatting.fixed(1.0 - p0, 4)
                + ".";
        return new Result(
            setup,
            "Nucleus measured in " + nb.name() + "; detector measured in " + db.name() + ".",
            List.copyOf(rows),
            probability(pe[0][0] + pe[1][1]),
            probability(pm[0][0] + pm[1][1]),
            alone,
            verdictBases,
            Formatting.fixed(conc, 6),
            Formatting.fixed(entangled.entanglementEntropy(), 6) + " bits",
            Formatting.fixed(entangled.reducedPurity(), 6),
            entanglementNote,
            settings,
            Formatting.fixed(sEntangled, 4),
            Formatting.fixed(sMixtureSame, 4),
            Formatting.fixed(sMixtureBest, 4),
            Formatting.fixed(maximum, 4),
            verdict,
            List.copyOf(curveE),
            List.copyOf(curveM));
    }

    /** Fixed decimals, scientific notation when small but not zero, and exact zero below rounding noise. */
    private static String probability(double p) {
        return TunnellingPresenter.probability(Math.abs(p) < NOISE ? 0.0 : p);
    }
}
