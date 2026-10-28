package dev.quantumlab.simulation;

import dev.quantumlab.math.Complex;
import dev.quantumlab.physics.QubitState;
import dev.quantumlab.physics.StandardBasis;

/**
 * Modes B and C configuration: a prepared qubit state, a measurement basis, and a seed.
 *
 * <p>The amplitudes are stored as plain doubles (rather than as a {@link QubitState}) so the record
 * serialises trivially and compares by value. The compact constructor rejects states that are not
 * normalised.
 *
 * @param alphaRe real part of {@code α}, the amplitude of {@code |0⟩} (not decayed)
 * @param alphaIm imaginary part of {@code α}
 * @param betaRe real part of {@code β}, the amplitude of {@code |1⟩} (decayed)
 * @param betaIm imaginary part of {@code β}
 * @param basis the observable to measure
 * @param seed seed of the pseudo-random generator
 */
public record QuantumMeasurementConfig(
        double alphaRe,
        double alphaIm,
        double betaRe,
        double betaIm,
        StandardBasis basis,
        long seed)
        implements ExperimentConfig {

    public static final String MODE_ID = "quantum-measurement";

    public QuantumMeasurementConfig {
        if (basis == null) {
            throw new IllegalArgumentException("basis must not be null");
        }
        // Throws if the amplitudes are non-finite or not normalised.
        QubitState.of(Complex.of(alphaRe, alphaIm), Complex.of(betaRe, betaIm));
    }

    public static QuantumMeasurementConfig of(QubitState state, StandardBasis basis, long seed) {
        return new QuantumMeasurementConfig(
                state.alpha().re(),
                state.alpha().im(),
                state.beta().re(),
                state.beta().im(),
                basis,
                seed);
    }

    public QubitState state() {
        return QubitState.of(Complex.of(alphaRe, alphaIm), Complex.of(betaRe, betaIm));
    }

    @Override
    public String modeId() {
        return MODE_ID;
    }
}
