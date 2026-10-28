package dev.quantumlab.physics;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexVector;
import dev.quantumlab.math.Tolerances;
import java.util.OptionalDouble;

/**
 * A pure state of a two-level quantum system, {@code |ψ⟩ = α|0⟩ + β|1⟩} with {@code |α|² + |β|² =
 * 1}.
 *
 * <p><b>Basis convention</b> (see ADR-0002): {@code |0⟩ = (1, 0)ᵀ} is the <em>undecayed</em> nucleus
 * and {@code |1⟩ = (0, 1)ᵀ} is the <em>decayed</em> nucleus. The pair {@code {|0⟩, |1⟩}} is the
 * "computational" (Z) basis. Amplitudes are stored in that order.
 *
 * <p>Instances are immutable and always normalised to within {@link Tolerances#NORMALISATION}.
 *
 * <p>A pure state is a ray, not a vector: {@code |ψ⟩} and {@code e^{iγ}|ψ⟩} describe the same
 * physical state. Probabilities and {@link #relativePhase()} are unaffected by the global phase
 * {@code γ}; the individual phases of {@code α} and {@code β} are.
 */
public final class QubitState {

    /** Below this modulus an amplitude's phase is numerically meaningless and treated as undefined. */
    private static final double PHASE_AMPLITUDE_FLOOR = 1e-12;

    private final ComplexVector vector;

    private QubitState(ComplexVector vector) {
        this.vector = vector;
    }

    // ---------------------------------------------------------------- factories

    /**
     * The state {@code α|0⟩ + β|1⟩}.
     *
     * @throws IllegalArgumentException if {@code |α|² + |β|²} differs from 1 by more than {@link
     *     Tolerances#NORMALISATION}
     */
    public static QubitState of(Complex alpha, Complex beta) {
        return fromVector(ComplexVector.of(alpha, beta));
    }

    /** The state proportional to {@code α|0⟩ + β|1⟩}, rescaled to unit norm. */
    public static QubitState normalizing(Complex alpha, Complex beta) {
        if (alpha.abs2() + beta.abs2() == 0.0) {
            throw new IllegalArgumentException("Cannot prepare a state from zero amplitudes");
        }
        return new QubitState(ComplexVector.of(alpha, beta).normalized());
    }

    /** Wraps a two-dimensional, normalised vector. */
    public static QubitState fromVector(ComplexVector v) {
        if (v.dimension() != 2) {
            throw new IllegalArgumentException(
                    "A qubit state has dimension 2, got " + v.dimension());
        }
        if (!v.isNormalized(Tolerances.NORMALISATION)) {
            throw new IllegalArgumentException(
                    "State is not normalised: |alpha|^2 + |beta|^2 = " + v.normSquared());
        }
        return new QubitState(v);
    }

    /** {@code |0⟩}: the nucleus has not decayed. */
    public static QubitState basis0() {
        return new QubitState(ComplexVector.basis(2, 0));
    }

    /** {@code |1⟩}: the nucleus has decayed. */
    public static QubitState basis1() {
        return new QubitState(ComplexVector.basis(2, 1));
    }

    /** {@code |+⟩ = (|0⟩ + |1⟩)/√2}. */
    public static QubitState plus() {
        double s = 1.0 / Math.sqrt(2.0);
        return new QubitState(ComplexVector.of(Complex.of(s), Complex.of(s)));
    }

    /** {@code |−⟩ = (|0⟩ − |1⟩)/√2}. */
    public static QubitState minus() {
        double s = 1.0 / Math.sqrt(2.0);
        return new QubitState(ComplexVector.of(Complex.of(s), Complex.of(-s)));
    }

    /** {@code |+i⟩ = (|0⟩ + i|1⟩)/√2}. */
    public static QubitState plusI() {
        double s = 1.0 / Math.sqrt(2.0);
        return new QubitState(ComplexVector.of(Complex.of(s), Complex.of(0, s)));
    }

    /** {@code |−i⟩ = (|0⟩ − i|1⟩)/√2}. */
    public static QubitState minusI() {
        double s = 1.0 / Math.sqrt(2.0);
        return new QubitState(ComplexVector.of(Complex.of(s), Complex.of(0, -s)));
    }

    /**
     * The state at Bloch-sphere angles {@code (θ, φ)}: {@code cos(θ/2)|0⟩ + e^{iφ} sin(θ/2)|1⟩}.
     * Here {@code P(|1⟩) = sin²(θ/2)} and {@code φ} is the relative phase between the two
     * amplitudes. Every pure qubit state is reachable this way up to a global phase.
     *
     * @param theta polar angle in {@code [0, π]}
     * @param phi azimuthal angle (any finite value; effectively taken mod 2π)
     */
    public static QubitState fromBloch(double theta, double phi) {
        if (!Double.isFinite(theta) || theta < 0.0 || theta > Math.PI) {
            throw new IllegalArgumentException("theta must lie in [0, pi], got " + theta);
        }
        if (!Double.isFinite(phi)) {
            throw new IllegalArgumentException("phi must be finite, got " + phi);
        }
        return new QubitState(
                ComplexVector.of(
                        Complex.of(Math.cos(theta / 2.0)),
                        Complex.cis(phi).times(Math.sin(theta / 2.0))));
    }

    /**
     * A <em>phenomenological</em> superposition whose Born probabilities reproduce the classical
     * decay law at time {@code t}: {@code |α|² = e^{−λt}} and {@code |β|² = 1 − e^{−λt}}, with the
     * relative phase chosen freely.
     *
     * <p>This is a modelling device for the thought experiment, not a derivation: a real unstable
     * nucleus is described by a far more complicated (Weisskopf–Wigner) treatment, and the relative
     * phase between "decayed" and "not decayed" is not something this model predicts.
     */
    public static QubitState fromDecay(DecayModel model, double t, double relativePhase) {
        if (!Double.isFinite(relativePhase)) {
            throw new IllegalArgumentException("relativePhase must be finite");
        }
        double survive = model.survivalProbability(t);
        double decay = model.decayProbability(t);
        return new QubitState(
                ComplexVector.of(
                        Complex.of(Math.sqrt(survive)),
                        Complex.cis(relativePhase).times(Math.sqrt(decay))));
    }

    // ---------------------------------------------------------------- accessors

    public Complex alpha() {
        return vector.get(0);
    }

    public Complex beta() {
        return vector.get(1);
    }

    public ComplexVector vector() {
        return vector;
    }

    /** {@code |α|²}: the probability of finding the nucleus undecayed in the Z basis. */
    public double probability0() {
        return alpha().abs2();
    }

    /** {@code |β|²}: the probability of finding the nucleus decayed in the Z basis. */
    public double probability1() {
        return beta().abs2();
    }

    /** {@code |α|² + |β|²}; equals 1 to within {@link Tolerances#NORMALISATION}. */
    public double normSquared() {
        return vector.normSquared();
    }

    /**
     * The relative phase {@code arg(β) − arg(α)} wrapped into {@code (−π, π]}. Unlike the phase of
     * either amplitude alone, it is unchanged by a global phase.
     *
     * <p>Empty when either amplitude is (numerically) zero, because then no relative phase exists.
     */
    public OptionalDouble relativePhase() {
        Complex a = alpha();
        Complex b = beta();
        if (a.abs() < PHASE_AMPLITUDE_FLOOR || b.abs() < PHASE_AMPLITUDE_FLOOR) {
            return OptionalDouble.empty();
        }
        double d = b.arg() - a.arg();
        while (d > Math.PI) {
            d -= 2.0 * Math.PI;
        }
        while (d <= -Math.PI) {
            d += 2.0 * Math.PI;
        }
        return OptionalDouble.of(d);
    }

    /**
     * True if both states are the same physical state, i.e. differ at most by a global phase:
     * {@code |⟨a|b⟩|² ≥ 1 − tol}.
     */
    public boolean sameRayAs(QubitState other, double tol) {
        return vector.innerProduct(other.vector).abs2() >= 1.0 - tol;
    }

    @Override
    public String toString() {
        return "(" + alpha() + ")|0> + (" + beta() + ")|1>";
    }
}
