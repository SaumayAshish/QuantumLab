package dev.quantumlab.simulation;

import java.util.List;

/**
 * The explanation shown beside a simulation mode: what the model is, the mathematics behind it, and
 * — just as prominently — what it does and does not represent.
 *
 * @param title the mode's name
 * @param summary a short plain-language description
 * @param mathematics the defining equations, in plain text with Unicode symbols
 * @param represents what the model genuinely captures
 * @param doesNotRepresent what it deliberately leaves out or must not be read as
 */
public record ModeExplanation(
        String title,
        String summary,
        String mathematics,
        List<String> represents,
        List<String> doesNotRepresent) {

    public ModeExplanation {
        represents = List.copyOf(represents);
        doesNotRepresent = List.copyOf(doesNotRepresent);
    }
}
