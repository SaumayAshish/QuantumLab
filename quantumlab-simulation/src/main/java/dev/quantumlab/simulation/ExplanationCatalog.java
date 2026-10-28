package dev.quantumlab.simulation;

import java.util.List;

/**
 * The explanatory text for each implemented mode.
 *
 * <p>Kept in code, not in the UI, so it can be tested: the scope caveats are part of the product's
 * scientific contract and a test fails if one is removed.
 */
public final class ExplanationCatalog {

    private ExplanationCatalog() {}

    public static ModeExplanation classical() {
        return new ModeExplanation(
                "Mode A — Classical thought experiment",
                "A radioactive nucleus either decays within the observation window or it does not. "
                        + "A detector registers a decay, which releases a mechanism that kills the "
                        + "cat. Here the whole chain is a classical stochastic process: one random "
                        + "number decides the nucleus's fate and everything else follows.",
                "P(decayed by t) = 1 − e^(−λt)\n"
                        + "Draw u ~ Uniform[0, 1); the nucleus has decayed iff u < P(decayed by t).\n"
                        + "Half-life t½ = ln 2 / λ.\n"
                        + "Detector, trigger and cat then follow deterministically from the nucleus.",
                List.of(
                        "A definite-but-unknown outcome: the nucleus has decayed or it has not, and "
                                + "the cat is alive or dead, throughout.",
                        "The exponential decay law with a configurable rate λ and observation time t.",
                        "A seeded pseudo-random draw, so a run can be repeated exactly."),
                List.of(
                        "This is not a quantum-state simulation: there are no amplitudes, no "
                                + "superposition and no interference.",
                        "It does not show a cat that is \"both alive and dead\". The randomness here "
                                + "is ignorance of a definite outcome, not superposition.",
                        "The detector is idealised: perfect efficiency, no noise, no dead time."));
    }

    public static ModeExplanation superposition() {
        return new ModeExplanation(
                "Mode B — Quantum superposition",
                "The nucleus is described by a complex state vector |ψ⟩ = α|0⟩ + β|1⟩. "
                        + "|0⟩ means \"not decayed\" and |1⟩ means \"decayed\". Prepare any state "
                        + "and inspect its amplitudes, normalisation, relative phase, and the "
                        + "probabilities the Born rule assigns in each measurement basis.",
                "|ψ⟩ = α|0⟩ + β|1⟩,  |α|² + |β|² = 1\n"
                        + "Born rule: p_k = |⟨b_k|ψ⟩|² for an orthonormal basis {|b_k⟩}.\n"
                        + "For (|0⟩ + e^(iφ)|1⟩)/√2:\n"
                        + "  Z basis: p = (½, ½) for every φ\n"
                        + "  X basis: P(+) = (1 + cos φ)/2\n"
                        + "  Y basis: P(+i) = (1 + sin φ)/2\n"
                        + "A global phase e^(iγ) multiplies both amplitudes and changes nothing "
                        + "observable; the relative phase arg β − arg α does.",
                List.of(
                        "A coherent superposition: its amplitudes carry phases, and the relative "
                                + "phase changes outcome probabilities in the X and Y bases.",
                        "The difference between a superposition and a classical mixture: a 50/50 "
                                + "mixture of |0⟩ and |1⟩ is 50/50 in every basis, whereas |+⟩ gives "
                                + "outcome |+⟩ with certainty in the X basis.",
                        "A two-level model of the nucleus only."),
                List.of(
                        "Preparing the state from the decay law sets |β|² = 1 − e^(−λt) by "
                                + "construction. That is a modelling convenience, not a derivation "
                                + "from nuclear physics, and the relative phase is a free choice.",
                        "Nothing evolves in time here: the state is prepared, not propagated by a "
                                + "Hamiltonian.",
                        "It says nothing about consciousness or about what \"observation\" means; "
                                + "measurement is a mathematical rule applied to the state."));
    }

    public static ModeExplanation measurement() {
        return new ModeExplanation(
                "Mode C — Measurement",
                "Each trial prepares a fresh copy of the state and performs a projective "
                        + "measurement in the chosen basis. A single result is random; only the "
                        + "distribution over many trials is predicted.",
                "Outcome k occurs with probability p_k = |⟨b_k|ψ⟩|²; afterwards the state is |b_k⟩.\n"
                        + "Sampling: draw one u ~ Uniform[0, 1) from a seeded SplitMix64 generator and "
                        + "return the first k with u < p_0 + … + p_k.\n"
                        + "Over N independent trials the observed frequency of outcome k has mean p_k and "
                        + "standard error √(p_k(1 − p_k)/N).",
                List.of(
                        "The Born rule as a probability distribution: an individual outcome is "
                                + "unpredictable and only frequencies are predicted.",
                        "The law of large numbers: observed frequencies converge to p_k, with "
                                + "scatter shrinking like 1/√N.",
                        "Reproducibility: the same seed replays the identical outcome sequence."),
                List.of(
                        "A seeded pseudo-random generator is deterministic. A reproducible sequence "
                                + "does not mean individual quantum outcomes are predictable; it "
                                + "means this program's random numbers can be replayed.",
                        "In the Z basis the idealised apparatus shows the cat's fate. In other "
                                + "bases the apparatus is not defined, so no cat outcome is shown.",
                        "Measurement is idealised and instantaneous: no apparatus dynamics, no "
                                + "decoherence, and no mechanism for collapse is modelled."));
    }

    /** Caveats that apply to the whole application. */
    public static ModeExplanation general() {
        return new ModeExplanation(
                "Scope of QuantumLab",
                "QuantumLab models a two-level quantum system and an idealised classical "
                        + "apparatus, so that the mathematics of superposition and measurement can "
                        + "be explored and tested.",
                "Every probability shown is computed from complex amplitudes by the Born rule or "
                        + "from the exponential decay law. Nothing is hard-coded or animated "
                        + "independently of the model.",
                List.of(
                        "Two-level state vectors with complex amplitudes.",
                        "Projective measurement in three bases, with reproducible sampling.",
                        "The classical decay law and an idealised detector → trigger → cat chain."),
                List.of(
                        "QuantumLab does not simulate a macroscopic cat and does not solve the "
                                + "quantum measurement problem.",
                        "It makes no claims about consciousness or about the nature of reality.",
                        "Entanglement of the nucleus with the apparatus (Mode D) and decoherence "
                                + "(Mode E) are planned but not implemented."));
    }
}
