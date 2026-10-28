package dev.quantumlab.simulation;

import dev.quantumlab.physics.ApparatusOutcome;
import dev.quantumlab.physics.QubitState;
import java.util.Optional;

/**
 * One recorded measurement trial.
 *
 * @param trialNumber 1-based position in the session
 * @param preState the state that was prepared and measured (identical for every trial of a session)
 * @param outcomeIndex which basis vector was found
 * @param outcomeLabel that vector's display label
 * @param theoreticalProbability the Born probability this outcome had
 * @param apparatus the detector/trigger/cat record, present only when the measurement is in the
 *     computational basis (the only basis in which the idealised apparatus is defined)
 */
public record TrialRecord(
        int trialNumber,
        QubitState preState,
        int outcomeIndex,
        String outcomeLabel,
        double theoreticalProbability,
        Optional<ApparatusOutcome> apparatus) {}
