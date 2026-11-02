package dev.quantumlab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatusControllerTest {

    @Test
    void reportsThatTheServiceIsUp() {
        StatusController.Status status = new StatusController().status();

        assertEquals("QuantumLab service", status.service());
        assertEquals("ok", status.status());
    }
}
