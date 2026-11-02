package dev.quantumlab.service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reports that the service is up. It returns no physics and makes no scientific claim. */
@RestController
@RequestMapping("/api")
public class StatusController {

    /** The JSON body of {@code GET /api/status}. */
    public record Status(String service, String status) {}

    @GetMapping("/status")
    public Status status() {
        return new Status("QuantumLab service", "ok");
    }
}
