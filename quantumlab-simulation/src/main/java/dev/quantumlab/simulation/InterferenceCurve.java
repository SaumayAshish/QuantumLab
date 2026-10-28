package dev.quantumlab.simulation;

import dev.quantumlab.math.Complex;
import dev.quantumlab.physics.BornRule;
import dev.quantumlab.physics.MeasurementBasis;
import dev.quantumlab.physics.QubitState;
import dev.quantumlab.physics.StandardBasis;
import java.util.ArrayList;
import java.util.List;

/**
 * How the Born probability of one outcome depends on the relative phase {@code φ} between the two
 * amplitudes.
 *
 * <p>Fixes the amplitude <em>magnitudes</em> {@code |α|, |β|} and sweeps the phase over {@code
 * [−π, π]}: {@code |ψ(φ)⟩ = |α||0⟩ + e^{iφ}|β||1⟩}. Every point is a genuine Born-rule evaluation
 * (no closed-form shortcut), so the curve is the simulator's own answer. In the computational basis
 * it is flat; in the X and Y bases it oscillates, which is the interference that distinguishes a
 * coherent superposition from a classical mixture.
 */
public final class InterferenceCurve {

    private InterferenceCurve() {}

    /**
     * @param alphaMagnitude {@code |α|}
     * @param betaMagnitude {@code |β|}; must satisfy {@code |α|² + |β|² = 1}
     * @param basis the measurement basis
     * @param outcomeIndex which outcome's probability to plot
     * @param points number of phase samples (≥ 2), evenly spaced from {@code −π} to {@code π}
     * @return points {@code (φ, p)}
     */
    public static List<SeriesPoint> sweep(
            double alphaMagnitude,
            double betaMagnitude,
            StandardBasis basis,
            int outcomeIndex,
            int points) {
        if (points < 2) {
            throw new IllegalArgumentException("points must be >= 2, got " + points);
        }
        MeasurementBasis b = basis.basis();
        if (outcomeIndex < 0 || outcomeIndex >= b.size()) {
            throw new IllegalArgumentException("outcomeIndex out of range: " + outcomeIndex);
        }
        List<SeriesPoint> curve = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double phi = -Math.PI + 2.0 * Math.PI * i / (points - 1);
            QubitState s =
                    QubitState.of(
                            Complex.of(alphaMagnitude),
                            Complex.cis(phi).times(betaMagnitude));
            curve.add(new SeriesPoint(phi, BornRule.probabilities(s, b)[outcomeIndex]));
        }
        return curve;
    }
}
