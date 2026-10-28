package dev.quantumlab.desktop;

/**
 * Plain main class for launching from an IDE. A main class that extends
 * {@code javafx.application.Application} fails on the classpath with "JavaFX runtime components
 * are missing"; starting through this class avoids that.
 */
public final class QuantumLabLauncher {

    private QuantumLabLauncher() {}

    public static void main(String[] args) {
        QuantumLabApp.main(args);
    }
}
