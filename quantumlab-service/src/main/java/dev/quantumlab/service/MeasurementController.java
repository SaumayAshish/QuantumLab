package dev.quantumlab.service;

import dev.quantumlab.physics.StandardBasis;
import dev.quantumlab.simulation.ExperimentExporter;
import dev.quantumlab.simulation.MeasurementSession;
import dev.quantumlab.simulation.QuantumMeasurementConfig;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Repeated seeded measurement of one prepared qubit. The answer is the same tested JSON export the
 * desktop application writes (schema {@code quantumlab.quantum-measurement/1}), including its scope
 * notes. The same inputs always give the same answer.
 */
@RestController
@RequestMapping("/api")
public class MeasurementController {

    /** Most trials one request may ask for. */
    static final int MAX_TRIALS = 100_000;

    /**
     * @param p probability of "not decayed", the squared magnitude of the amplitude of |0⟩ (0 to 1)
     * @param phase relative phase between the two amplitudes, in radians (optional, default 0)
     * @param basis Z, X or Y
     * @param seed seed of the random generator (a whole number)
     * @param trials how many fresh copies to prepare and measure (1 to {@value #MAX_TRIALS})
     */
    @GetMapping(value = "/measurement", produces = "application/json;charset=UTF-8")
    public String measurement(
        @RequestParam(name = "p", required = false) String p,
        @RequestParam(name = "phase", required = false) String phase,
        @RequestParam(name = "basis", required = false) String basis,
        @RequestParam(name = "seed", required = false) String seed,
        @RequestParam(name = "trials", required = false) String trials) {
        double notDecayed = Numbers.parse(p, "p");
        if (notDecayed < 0.0 || notDecayed > 1.0) {
            throw new BadRequestException("p must be between 0 and 1, got " + p);
        }
        double relativePhase = phase == null ? 0.0 : Numbers.parse(phase, "phase");
        StandardBasis standardBasis = parseBasis(basis);
        long seedValue = Numbers.parseLong(seed, "seed");
        long trialCount = Numbers.parseLong(trials, "trials");
        if (trialCount < 1 || trialCount > MAX_TRIALS) {
            throw new BadRequestException(
                "trials must be between 1 and " + MAX_TRIALS + ", got " + trials);
        }

        try {
            double alpha = Math.sqrt(notDecayed);
            double beta = Math.sqrt(1.0 - notDecayed);
            QuantumMeasurementConfig config =
                new QuantumMeasurementConfig(
                    alpha,
                    0.0,
                    beta * Math.cos(relativePhase),
                    beta * Math.sin(relativePhase),
                    standardBasis,
                    seedValue);
            MeasurementSession session = new MeasurementSession(config);
            session.measure((int) trialCount);
            return ExperimentExporter.toJson(session);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private static StandardBasis parseBasis(String text) {
        if (text == null) {
            throw new BadRequestException("basis is required");
        }
        return switch (text.trim().toUpperCase(Locale.ROOT)) {
            case "Z" -> StandardBasis.COMPUTATIONAL;
            case "X" -> StandardBasis.HADAMARD;
            case "Y" -> StandardBasis.CIRCULAR;
            default -> throw new BadRequestException("basis must be Z, X or Y, got '" + text + "'");
        };
    }
}
