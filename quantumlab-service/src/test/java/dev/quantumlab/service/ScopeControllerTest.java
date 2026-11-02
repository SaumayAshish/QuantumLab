package dev.quantumlab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.quantumlab.simulation.ExplanationCatalog;
import org.junit.jupiter.api.Test;

class ScopeControllerTest {

    @Test
    void returnsTheGeneralExplanationFromTheCatalogue() {
        assertEquals(ExplanationCatalog.general(), new ScopeController().scope());
    }

    @Test
    void statesWhatTheModelDoesNotRepresent() {
        assertFalse(new ScopeController().scope().doesNotRepresent().isEmpty());
    }
}
