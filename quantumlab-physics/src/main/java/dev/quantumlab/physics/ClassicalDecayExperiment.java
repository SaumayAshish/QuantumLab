package dev.quantumlab.physics;

import java.util.random.RandomGenerator;

/**
 * Mode A: the thought experiment as a <em>classical stochastic</em> process.
 *
 * <p>One uniform random number decides whether the nucleus decayed in the window; the apparatus
 * then follows deterministically. There are no amplitudes, no superposition and no interference
 * here — only ignorance about a definite outcome. This is exactly why Mode A is not a quantum
 * simulation: the cat is either alive or dead throughout, and we merely do not know which.
 */
public final class ClassicalDecayExperiment {

    private ClassicalDecayExperiment() {}

    /**
     * One classical run.
     *
     * @param decayProbability {@code P(decayed by t)} used for the draw
     * @param draw the uniform variate in {@code [0, 1)} that was compared against it
     * @param outcome the resulting apparatus record; {@code nucleusDecayed ⇔ draw < decayProbability}
     */
    public record ClassicalRun(double decayProbability, double draw, ApparatusOutcome outcome) {}

    public static ClassicalRun run(DecayModel model, double observationTime, RandomGenerator rng) {
        double p = model.decayProbability(observationTime);
        double u = rng.nextDouble();
        return new ClassicalRun(p, u, ApparatusOutcome.consequenceOf(u < p));
    }
}
