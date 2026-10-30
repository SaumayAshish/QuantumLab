package dev.quantumlab.simulation;

import java.util.List;

/**
 * The explanatory text for each implemented mode.
 *
 * <p>Kept in code, not in the UI, so it can be tested: the scope caveats are part of the product's
 * scientific contract and a test fails if one is removed.
 */
public final class ExplanationCatalog {

    private ExplanationCatalog() {
    }

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

    public static ModeExplanation tunnelling() {
        return new ModeExplanation(
            "Tunnelling — transmission through a barrier",
            "A particle with energy E arrives at a potential barrier from the left. This screen "
                + "computes the probability T that it is found on the far side, for one "
                + "definite energy at a time, and plots T against E.",
            "−½ ψ″(x) + V(x) ψ(x) = E ψ(x)   (ħ = m = 1), V = 0 outside the barrier.\n"
                + "Inside a slab of constant V the pair (ψ, ψ') is carried across by a 2×2 "
                + "matrix; the slab matrices are multiplied and T follows from the result.\n"
                + "E < V0: κ = √(2(V0 − E)), T = 1 / (1 + V0² sinh²(κa) / (4E(V0 − E))).\n"
                + "E > V0: k = √(2(E − V0)), T = 1 / (1 + V0² sin²(ka) / (4E(E − V0))).\n"
                + "Thick barrier (κa ≫ 1): T ≈ 16E(V0 − E)/V0² · e^(−2κa).\n"
                + "Reflection R = 1 − T.",
            List.of(
                "The long-time transmission probability of a wave of one definite energy "
                    + "through one or two rectangular barriers.",
                "Tunnelling: T is above zero for E < V0, where a classical particle is "
                    + "always turned back.",
                "Reflection above the barrier: T is below 1 for E > V0, where a classical "
                    + "particle always gets over, and T reaches 1 only at isolated "
                    + "energies.",
                "Resonances: two barriers can transmit well at energies where one barrier "
                    + "transmits poorly."),
            List.of(
                "Nothing moves: this is a stationary calculation, not a wave packet "
                    + "travelling through the barrier, and no time of passage or speed "
                    + "of tunnelling is shown.",
                "No measurement or collapse is shown, and no individual particle is "
                    + "followed: T is a probability.",
                "An idealised model: sharp rectangular barriers in one dimension, "
                    + "ħ = m = 1, no real material or device.",
                "Probabilities below about 1e-300 are reported as 0."));
    }

    public static ModeExplanation schrodinger1d() {
        return new ModeExplanation(
            "Quantum well — stationary states in one dimension",
            "A single particle in one dimension, in a chosen potential, has energy eigenstates "
                + "with sharp energies. This screen finds them numerically by replacing the "
                + "second derivative with finite differences, and compares each energy with "
                + "the exact value where one is known.",
            "−½ ψ″(x) + V(x) ψ(x) = E ψ(x)   (ħ = m = 1), with ψ = 0 at both walls.\n"
                + "On a grid of spacing h: ψ″(x_i) ≈ (ψ_{i−1} − 2ψ_i + ψ_{i+1}) / h².\n"
                + "This is a matrix with 1/h² + V(x_i) on the diagonal and −1/(2h²) beside it; "
                + "its eigenvalues are the energies.\n"
                + "Infinite well of width L: E_n = n²π²/(2L²), ψ_n = √(2/L) sin(nπx/L), "
                + "n = 1, 2, …\n"
                + "Harmonic oscillator V = ½ω²x²: E_n = ω(n + ½), n = 0, 1, …\n"
                + "Normalisation: Σ ψ_i² h = 1, so |ψ|² is a probability density.",
            List.of(
                "Energy levels and stationary states of one particle in a fixed "
                    + "one-dimensional potential, computed numerically.",
                "Discretisation error made visible: the table gives numerical minus exact "
                    + "energy for every level, and the error shrinks like h² as the grid "
                    + "is refined.",
                "Nodes: the state n steps above the ground state has n nodes.",
                "A closed-form check: for the infinite well the numerical states match the "
                    + "exact wavefunctions."),
            List.of(
                "Nothing evolves in time. These are stationary states, not a moving wave "
                    + "packet, and no measurement or collapse is shown.",
                "The walls are hard: the wavefunction is forced to zero at both ends. For "
                    + "the oscillator they form an artificial box, so levels whose "
                    + "classical range reaches the walls are pushed up.",
                "Only the lowest few levels are accurate: high levels vary quickly and the "
                    + "finite-difference error grows with the level number.",
                "A single particle in natural units (ħ = m = 1), not a specific physical "
                    + "system, and not the nucleus or the cat of the other modes."));
    }

    /**
     * Caveats that apply to the whole application.
     */
    /**
     * Caveats that apply to the whole application.
     */
    public static ModeExplanation general() {
        return new ModeExplanation(
            "Scope of QuantumLab",
            "QuantumLab models a two-level quantum system with an idealised classical "
                + "apparatus, and, separately, a single particle in one dimension, so that "
                + "the mathematics of superposition, measurement and stationary states can "
                + "be explored and tested.",
            "Every probability shown is computed from complex amplitudes by the Born rule or "
                + "from the exponential decay law, and every energy level from the "
                + "finite-difference Schrödinger equation. Nothing is hard-coded or "
                + "animated independently of the model.",
            List.of(
                "Two-level state vectors with complex amplitudes.",
                "Projective measurement in three bases, with reproducible sampling.",
                "The classical decay law and an idealised detector → trigger → cat chain.",
                "Energy levels and stationary states of one particle in one dimension, by "
                    + "finite differences.",
                "Stationary transmission of a particle through rectangular barriers, by "
                    + "transfer matrices."),
            List.of(
                "QuantumLab does not simulate a macroscopic cat and does not solve the "
                    + "quantum measurement problem.",
                "It makes no claims about consciousness or about the nature of reality.",
                "The cat experiment, the one-dimensional solver and the tunnelling screen are "
                    + "separate models: the solver and the tunnelling screen do not "
                    + "describe the cat or its nucleus.",
                "Entanglement of the nucleus with the apparatus (Mode D) and decoherence "
                    + "(Mode E) are planned but not implemented."));
    }
}
