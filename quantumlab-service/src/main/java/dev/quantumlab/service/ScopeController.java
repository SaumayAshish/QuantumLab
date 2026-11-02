package dev.quantumlab.service;

import dev.quantumlab.simulation.ExplanationCatalog;
import dev.quantumlab.simulation.ModeExplanation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the general explanation, with what the laboratory does and does not represent.
 * The text comes from the same tested catalogue as the desktop screens.
 */
@RestController
@RequestMapping("/api")
public class ScopeController {

    @GetMapping("/scope")
    public ModeExplanation scope() {
        return ExplanationCatalog.general();
    }
}
