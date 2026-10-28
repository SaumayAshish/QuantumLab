package dev.quantumlab.simulation;

import dev.quantumlab.physics.DecayModel;

/**
 * Mode A configuration: a classical radioactive-decay experiment.
 *
 * @param decayRate the decay constant {@code λ ≥ 0}, in inverse model time units
 * @param observationTime the observation window {@code t ≥ 0}, in model time units
 * @param seed seed of the pseudo-random generator
 */
public record ClassicalDecayConfig(double decayRate, double observationTime, long seed)
        implements ExperimentConfig {

    public static final String MODE_ID = "classical-decay";

    public ClassicalDecayConfig {
        // Validation is delegated to the model so the rules live in one place.
        DecayModel model = new DecayModel(decayRate);
        model.decayProbability(observationTime);
    }

    public DecayModel model() {
        return new DecayModel(decayRate);
    }

    /** {@code P(decayed by t)}. */
    public double decayProbability() {
        return model().decayProbability(observationTime);
    }

    @Override
    public String modeId() {
        return MODE_ID;
    }
}
