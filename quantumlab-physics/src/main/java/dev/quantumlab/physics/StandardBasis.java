package dev.quantumlab.physics;

/**
 * The three standard qubit measurement bases, as a closed set suitable for UI selection and for
 * serialising an experiment configuration by name.
 */
public enum StandardBasis {
    /** {@code {|0⟩, |1⟩}} — the basis in which the apparatus records "decayed / not decayed". */
    COMPUTATIONAL,
    /** {@code {|+⟩, |−⟩}} — sensitive to the relative phase between {@code α} and {@code β}. */
    HADAMARD,
    /** {@code {|+i⟩, |−i⟩}} — sensitive to the relative phase, shifted by a quarter turn. */
    CIRCULAR;

    public MeasurementBasis basis() {
        return switch (this) {
            case COMPUTATIONAL -> MeasurementBasis.computational();
            case HADAMARD -> MeasurementBasis.hadamard();
            case CIRCULAR -> MeasurementBasis.circular();
        };
    }
}
