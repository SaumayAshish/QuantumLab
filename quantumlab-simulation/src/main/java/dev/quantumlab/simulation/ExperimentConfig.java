package dev.quantumlab.simulation;

/**
 * The complete, serialisable description of an experiment. Together with the software version it
 * is sufficient to reproduce every result exactly.
 *
 * <p>The hierarchy is closed: a new experiment kind must be added here, which forces every
 * {@code switch} over configurations (export, UI) to handle it.
 */
public sealed interface ExperimentConfig permits ClassicalDecayConfig, QuantumMeasurementConfig {

    /** Seed of the pseudo-random generator driving the experiment. */
    long seed();

    /** A stable machine-readable name, used in exports. */
    String modeId();
}
