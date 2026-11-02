package dev.quantumlab.service;

import dev.quantumlab.physics.DecayModel;
import dev.quantumlab.simulation.ExplanationCatalog;
import dev.quantumlab.simulation.ModeExplanation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The classical exponential decay law for one rate and one time. No randomness. */
@RestController
@RequestMapping("/api")
public class DecayController {

    /**
     * The JSON body of {@code GET /api/decay}. Time is in model time units and the rate in inverse
     * model time units. {@code halfLife} is null for a stable nucleus (rate 0), because JSON has no
     * infinity.
     */
    public record DecayResult(
        double decayRate,
        double time,
        double survivalProbability,
        double decayProbability,
        Double halfLife,
        ModeExplanation explanation) {}

    @GetMapping("/decay")
    public DecayResult decay(
        @RequestParam(name = "rate", required = false) String rate,
        @RequestParam(name = "time", required = false) String time) {
        double lambda = Numbers.parse(rate, "rate");
        double t = Numbers.parse(time, "time");
        try {
            DecayModel model = new DecayModel(lambda);
            double halfLife = model.halfLife();
            return new DecayResult(
                lambda,
                t,
                model.survivalProbability(t),
                model.decayProbability(t),
                Double.isInfinite(halfLife) ? null : halfLife,
                ExplanationCatalog.classical());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
