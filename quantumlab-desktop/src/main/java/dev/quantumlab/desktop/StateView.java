package dev.quantumlab.desktop;

import dev.quantumlab.physics.QubitState;
import dev.quantumlab.simulation.Formatting;
import dev.quantumlab.simulation.MeasurementSession;
import java.util.ArrayList;
import java.util.List;

/**
 * The prepared state, already formatted for display. Immutable, so it can be handed from a worker
 * thread to the UI thread safely.
 *
 * @param alpha amplitude of |0⟩ (not decayed)
 * @param beta amplitude of |1⟩ (decayed)
 * @param probability0 Born probability of |0⟩ in the computational basis
 * @param probability1 Born probability of |1⟩ in the computational basis
 * @param relativePhase phase of β relative to α, or a note that it is undefined
 * @param basisLabels outcome labels of the chosen measurement basis
 * @param basisProbabilities Born probability of each outcome in the chosen basis
 * @param note a message about how the input was adjusted (empty if it was not)
 */
public record StateView(
        String alpha,
        String beta,
        String probability0,
        String probability1,
        String relativePhase,
        List<String> basisLabels,
        List<String> basisProbabilities,
        String note) {

    public StateView {
        basisLabels = List.copyOf(basisLabels);
        basisProbabilities = List.copyOf(basisProbabilities);
    }

    static StateView of(MeasurementSession session, String note) {
        QubitState s = session.preparedState();
        double[] born = session.theoreticalProbabilities();
        List<String> probabilities = new ArrayList<>(born.length);
        for (double p : born) {
            probabilities.add(Formatting.percent(p));
        }
        return new StateView(
                Formatting.complex(s.alpha(), 4),
                Formatting.complex(s.beta(), 4),
                Formatting.percent(s.probability0()),
                Formatting.percent(s.probability1()),
                Formatting.phase(s.relativePhase()),
                session.labels(),
                probabilities,
                note);
    }
}
