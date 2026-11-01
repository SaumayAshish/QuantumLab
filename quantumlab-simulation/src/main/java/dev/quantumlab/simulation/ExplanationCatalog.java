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

    public static ModeExplanation wavePacket() {
        return new ModeExplanation(
            "Wave packet — a particle meeting a barrier in time",
            "A single particle in one dimension starts as a Gaussian wave packet to the left of a "
                + "rectangular barrier and moves towards it. This screen steps the "
                + "time-dependent Schrödinger equation forward and shows the probability "
                + "density splitting into a reflected part and a transmitted part, then "
                + "compares the final split with the stationary transmission T(E) averaged "
                + "over the packet's energies.",
            "i ∂ψ/∂t = −½ ψ″ + V(x) ψ   (ħ = m = 1), with ψ = 0 at walls at x = ±80.\n"
                + "Initial packet: ψ ∝ exp(−(x − x0)² / 4σ²) · e^{i k0 x}, so |ψ|² has "
                + "standard deviation σ and the mean momentum is k0.\n"
                + "Crank–Nicolson step: (1 + i·dt/2·H) ψ(t+dt) = (1 − i·dt/2·H) ψ(t). Each "
                + "step conserves Σ|ψ|²h to rounding, for any dt.\n"
                + "Prediction for the late-time probability beyond the barrier: "
                + "∫ |φ(k)|² T(k²/2) dk with |φ(k)|² ∝ exp(−2σ²(k − k0)²).",
            List.of(
                "How the probability density of one particle evolves in time under the "
                    + "Schrödinger equation, including the split into a reflected and a "
                    + "transmitted part.",
                "Conservation checks: the total probability stays 1 and the mean energy is "
                    + "conserved by the numerical scheme.",
                "Agreement with stationary theory: the probability beyond the barrier at "
                    + "late time matches T(E) averaged over the packet's spread of "
                    + "energies, not T at the mean energy alone."),
            List.of(
                "A numerical solution on a grid (spacing 0.1) with hard walls at ±80: "
                    + "results depend on the grid, and the barrier width is rounded to "
                    + "a whole number of cells.",
                "No measurement or collapse is shown: the animation is the density |ψ|², "
                    + "and a measurement would find the particle on one side, with "
                    + "these probabilities.",
                "The packet is not a classical particle splitting in two: no trajectory of "
                    + "an individual particle is followed, and no passage time or speed "
                    + "of tunnelling is defined or shown.",
                "An idealised model: a sharp rectangular barrier in one dimension, "
                    + "ħ = m = 1, not a specific material or device, and not the cat "
                    + "or its nucleus."));
    }

    /**
     * Mode D: a nucleus and an idealised detector as one two-qubit state.
     */
    public static ModeExplanation composite() {
        return new ModeExplanation(
            "Composite system — a nucleus and a detector as one state",
            "Two two-level systems, a nucleus and an idealised detector, are described by one "
                + "joint state. This screen compares the entangled state α|00⟩ + β|11⟩ with "
                + "the classical mixture that has the same chances of \"not decayed\" and "
                + "\"decayed\", by their joint measurement statistics in chosen bases and by "
                + "the CHSH quantity.",
            "Joint state in C⁴ = C² ⊗ C²: |ψ⟩ = α|00⟩ + β|11⟩, α = √p0, β = √(1 − p0) e^{iφ}.\n"
                + "Joint Born rule: P(i, j) = |⟨bᵢ ⊗ cⱼ | ψ⟩|² for the nucleus basis {bᵢ} and "
                + "the detector basis {cⱼ}.\n"
                + "Classical mixture: ρ = p0 |00⟩⟨00| + (1 − p0) |11⟩⟨11|, which has no "
                + "amplitudes to interfere.\n"
                + "Concurrence C = 2|a₀₀a₁₁ − a₀₁a₁₀|; entropy of either qubit alone "
                + "−Σ λ log₂ λ with λ = (1 ± √(1 − C²))/2.\n"
                + "CHSH: S = E(a,b) + E(a,b′) + E(a′,b) − E(a′,b′), with |S| ≤ 2 for any "
                + "shared classical variable and |S| ≤ 2√2 for quantum states; for this "
                + "state the largest S is 2√(1 + C²).",
            List.of(
                "How two two-level systems are described by one joint state, and how an "
                    + "entangled state differs from a classical mixture that gives "
                    + "identical statistics when both are measured in the computational "
                    + "basis.",
                "Exact joint Born-rule probabilities for projective measurements of both "
                    + "systems in the Z, X or Y basis, and the fact that each system "
                    + "on its own looks the same for the entangled state and the "
                    + "mixture: the difference is in the correlations.",
                "The concurrence and the entanglement entropy of the pure joint state, and "
                    + "the CHSH value with its classical bound 2 and Tsirelson's bound "
                    + "2√2."),
            List.of(
                "The detector is a two-level stand-in, not a model of a real detector: "
                    + "it has no macroscopic degrees of freedom and no amplification.",
                "Not the cat: no macroscopic cat is modelled and a cat is not treated as "
                    + "a qubit. The state is written down, not produced by an "
                    + "interaction.",
                "No dynamics and no decoherence: nothing evolves in time on this screen "
                    + "(the decoherence screen dephases a single qubit, not this pair).",
                "The probabilities are exact, not sampled, and no single outcome or "
                    + "collapse is shown. This does not solve the measurement problem "
                    + "and favours no interpretation.",
                "A CHSH value above 2 rules out outcomes fixed in advance by a shared "
                    + "classical variable for these two qubits. It says nothing about "
                    + "consciousness or about a cat, and no spatial separation or "
                    + "signalling between the two systems is modelled."));
    }

    /**
     * Mode E: a qubit losing its coherence.
     */
    public static ModeExplanation decoherence() {
        return new ModeExplanation(
            "Decoherence — a qubit losing its coherence",
            "A single two-level system, such as the nucleus, starts in a pure superposition and "
                + "is then dephased at a rate you choose: the interference between its two "
                + "outcomes fades, while the chances of \"not decayed\" and \"decayed\" do "
                + "not change. This screen shows how the statistics in each basis, the "
                + "interference fringe and the purity change with time.",
            "Density matrix ρ = |ψ⟩⟨ψ|: ρ₀₀ = |α|², ρ₁₁ = |β|², ρ₀₁ = αβ*.\n"
                + "Pure dephasing at rate γ: ρ₀₁(t) = ρ₀₁(0) e^{−γt}, with ρ₀₀ and ρ₁₁ "
                + "unchanged. Equivalently, a random Z flip with probability "
                + "q = (1 − e^{−γt})/2: ρ → (1 − q) ρ + q ZρZ.\n"
                + "Born rule for a mixed state: P(k) = ⟨bₖ|ρ|bₖ⟩.\n"
                + "Interference fringe: P(φ) = ½ + Re(ρ₀₁ e^{iφ}) for the outcome "
                + "(|0⟩ + e^{iφ}|1⟩)/√2; its visibility is 2|ρ₀₁|.\n"
                + "Purity Tr ρ² = ρ₀₀² + ρ₁₁² + 2|ρ₀₁|²; entropy −Σ λ log₂ λ with "
                + "λ = (1 ± r)/2, where r is the length of the Bloch vector.",
            List.of(
                "How a superposition becomes, statistically, indistinguishable from a "
                    + "classical mixture of its two outcomes when its coherence decays, "
                    + "while the Z-basis chances stay as they were.",
                "Exact probabilities in the Z, X or Y basis at t = 0, at the chosen time "
                    + "and in the long-time limit, with the visibility of the "
                    + "interference fringe and the purity and entropy of the qubit.",
                "The difference between losing coherence and a single outcome appearing: "
                    + "the state stays a density matrix of probabilities."),
            List.of(
                "The dephasing rate is an input you choose, not derived from any physical "
                    + "environment: this is a phenomenological model of one qubit, and no "
                    + "environment is simulated.",
                "Not the cat: no macroscopic cat is modelled and a cat is not treated as a "
                    + "qubit. Decoherence of a real cat would involve an enormous number "
                    + "of environmental degrees of freedom that are not simulated here.",
                "Decoherence does not by itself explain why one outcome is observed: no "
                    + "single outcome or collapse is shown. This does not solve the "
                    + "measurement problem and favours no interpretation.",
                "Only pure dephasing is modelled: no energy relaxation (the Z-basis chances "
                    + "never change), no motion under the qubit's own Hamiltonian and no "
                    + "revival of coherence.",
                "The probabilities are exact, not sampled, and nothing here concerns "
                    + "consciousness."));
    }

    /**
     * The interference curve: probability against the relative phase of one qubit.
     */
    public static ModeExplanation interference() {
        return new ModeExplanation(
            "Interference — how the relative phase changes the statistics",
            "A qubit is prepared with fixed chances of \"not decayed\" and \"decayed\" and a relative "
                + "phase φ between its two amplitudes that is swept through a full turn. This screen "
                + "plots the Born probability of an outcome against φ in the chosen basis and "
                + "compares it with the classical mixture that has the same chances in the Z basis.",
            "|ψ(φ)⟩ = |α||0⟩ + e^{iφ}|β||1⟩,  |α|² = P(not decayed), |β|² = 1 − |α|².\n"
                + "Z basis: p = (|α|², |β|²) for every φ.\n"
                + "X basis: P(+) = (1 + 2|α||β| cos φ)/2.\n"
                + "Y basis: P(+i) = (1 + 2|α||β| sin φ)/2.\n"
                + "Classical mixture ρ = |α|²|0⟩⟨0| + |β|²|1⟩⟨1|: P(k) = ⟨bₖ|ρ|bₖ⟩, which does not "
                + "depend on φ and equals ½ in the X and Y bases.\n"
                + "Fringe height = largest minus smallest probability over φ: 2|α||β| in the X and "
                + "Y bases, 0 in the Z basis.",
            List.of(
                "How the relative phase between two amplitudes, which Z-basis statistics cannot "
                    + "show, changes the probabilities in the X and Y bases: interference.",
                "The difference between a coherent superposition and a classical mixture with the "
                    + "same chances of \"not decayed\" and \"decayed\": the mixture gives the same "
                    + "answer for every phase.",
                "The height of the fringe, 2|α||β|: largest for equal chances and zero for a "
                    + "state that is purely \"not decayed\" or purely \"decayed\"."),
            List.of(
                "The phase is a free input swept through a full turn, not derived from any "
                    + "physics of the nucleus: no physical process sets or changes it here.",
                "Nothing is sampled, measured or collapsed: every point is an exact Born-rule "
                    + "probability, and no single outcome is shown.",
                "Nothing evolves in time: the loss of the fringe through dephasing is on the "
                    + "decoherence screen, not here.",
                "Not the cat: no macroscopic cat is modelled and a cat is not treated as a "
                    + "qubit. This is a two-level model of the nucleus only.",
                "The classical mixture is a density-matrix calculation, not a simulation of an "
                    + "apparatus: it does not say whether or how a real superposition becomes a "
                    + "mixture."));
    }

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
                    + "transfer matrices.",
                "The motion in time of a Gaussian wave packet meeting a rectangular "
                    + "barrier, by Crank–Nicolson stepping.",
                "Two qubits, a nucleus and an idealised detector, as an entangled state or "
                    + "a classical mixture, compared by joint measurement statistics "
                    + "and the CHSH quantity.",
                "One qubit losing its coherence at an assumed dephasing rate, and what that "
                    + "does to its statistics, its interference fringe and its purity.",
                "One qubit with a chosen relative phase between its amplitudes, and how its "
                    + "probabilities in each basis change with that phase, compared with a "
                    + "classical mixture."),
            List.of(
                "QuantumLab does not simulate a macroscopic cat and does not solve the "
                    + "quantum measurement problem.",
                "It makes no claims about consciousness or about the nature of reality.",
                "The cat experiment, the one-dimensional solver and the tunnelling screen are "
                    + "separate models: the solver and the tunnelling screen do not "
                    + "describe the cat or its nucleus.",
                "The wave-packet screen is likewise a separate model and does not describe "
                    + "the cat or its nucleus.",
                "The composite-system screen is a separate two-qubit model: its detector "
                    + "is a two-level stand-in and it does not describe the cat or a "
                    + "real apparatus.",
                               "The decoherence screen is a separate single-qubit model with an assumed "
                    + "dephasing rate: it does not describe the cat, a real environment "
                    + "or the appearance of a single outcome.",
                "The interference screen is a separate single-qubit model with a free phase: "
                    + "it does not describe the cat, a real apparatus or any process that "
                    + "sets the phase."));
    }
}
