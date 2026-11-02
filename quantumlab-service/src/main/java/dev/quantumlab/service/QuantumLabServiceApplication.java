package dev.quantumlab.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the optional HTTP service. The desktop application does not use it. */
@SpringBootApplication
public class QuantumLabServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuantumLabServiceApplication.class, args);
    }
}
