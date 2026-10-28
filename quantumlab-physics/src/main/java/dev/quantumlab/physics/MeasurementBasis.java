package dev.quantumlab.physics;

import dev.quantumlab.math.Complex;
import dev.quantumlab.math.ComplexVector;
import dev.quantumlab.math.Tolerances;
import java.util.List;

/**
 * An orthonormal basis defining a projective measurement: outcome {@code k} corresponds to the
 * projector {@code |bₖ⟩⟨bₖ|}.
 *
 * <p>The constructor verifies orthonormality ({@code ⟨bᵢ|bⱼ⟩ = δᵢⱼ} within {@link
 * Tolerances#NORMALISATION}) and that the basis is complete (as many vectors as dimensions), so
 * the Born probabilities of any normalised state are guaranteed to sum to one.
 *
 * <p>The three standard qubit bases are available as {@link #computational()}, {@link
 * #hadamard()}, and {@link #circular()}.
 */
public final class MeasurementBasis {

    private final String name;
    private final List<String> labels;
    private final List<ComplexVector> vectors;

    /**
     * @param name a display name for the basis
     * @param labels one display label per outcome
     * @param vectors the basis vectors, in outcome order
     * @throws IllegalArgumentException if the vectors are not a complete orthonormal basis
     */
    public MeasurementBasis(String name, List<String> labels, List<ComplexVector> vectors) {
        if (vectors.isEmpty()) {
            throw new IllegalArgumentException("A basis needs at least one vector");
        }
        int dim = vectors.get(0).dimension();
        if (vectors.size() != dim) {
            throw new IllegalArgumentException(
                    "A complete basis of dimension " + dim + " needs " + dim + " vectors, got "
                            + vectors.size());
        }
        if (labels.size() != vectors.size()) {
            throw new IllegalArgumentException("Need one label per basis vector");
        }
        for (int i = 0; i < dim; i++) {
            if (vectors.get(i).dimension() != dim) {
                throw new IllegalArgumentException("All basis vectors must share one dimension");
            }
            for (int j = 0; j < dim; j++) {
                Complex ip = vectors.get(i).innerProduct(vectors.get(j));
                Complex expected = i == j ? Complex.ONE : Complex.ZERO;
                if (!ip.approxEquals(expected, Tolerances.NORMALISATION)) {
                    throw new IllegalArgumentException(
                            "Basis is not orthonormal: <b" + i + "|b" + j + "> = " + ip);
                }
            }
        }
        this.name = name;
        this.labels = List.copyOf(labels);
        this.vectors = List.copyOf(vectors);
    }

    /** The Z basis {@code {|0⟩, |1⟩}}: "not decayed" / "decayed". */
    public static MeasurementBasis computational() {
        return new MeasurementBasis(
                "Computational (Z)",
                List.of("|0⟩", "|1⟩"),
                List.of(ComplexVector.basis(2, 0), ComplexVector.basis(2, 1)));
    }

    /** The X basis {@code {|+⟩, |−⟩}}, where {@code |±⟩ = (|0⟩ ± |1⟩)/√2}. */
    public static MeasurementBasis hadamard() {
        return new MeasurementBasis(
                "Hadamard (X)",
                List.of("|+⟩", "|−⟩"),
                List.of(QubitState.plus().vector(), QubitState.minus().vector()));
    }

    /** The Y basis {@code {|+i⟩, |−i⟩}}, where {@code |±i⟩ = (|0⟩ ± i|1⟩)/√2}. */
    public static MeasurementBasis circular() {
        return new MeasurementBasis(
                "Circular (Y)",
                List.of("|+i⟩", "|−i⟩"),
                List.of(QubitState.plusI().vector(), QubitState.minusI().vector()));
    }

    public String name() {
        return name;
    }

    /** Number of outcomes, which equals the dimension of the space. */
    public int size() {
        return vectors.size();
    }

    public int dimension() {
        return vectors.get(0).dimension();
    }

    public String label(int k) {
        return labels.get(k);
    }

    public ComplexVector vector(int k) {
        return vectors.get(k);
    }

    @Override
    public String toString() {
        return name + " " + labels;
    }
}
