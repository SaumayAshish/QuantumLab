package dev.quantumlab.physics.scattering;

/**
 * A slab of constant potential energy in a one-dimensional scattering problem (ħ = m = 1).
 *
 * @param potential the potential energy inside the slab; any finite value, negative for a well
 * @param width the thickness of the slab, positive
 */
public record Layer(double potential, double width) {

    public Layer {
        if (!Double.isFinite(potential)) {
            throw new IllegalArgumentException("The potential must be a finite number.");
        }
        if (!Double.isFinite(width) || width <= 0.0) {
            throw new IllegalArgumentException("The width must be a positive, finite number.");
        }
    }
}
