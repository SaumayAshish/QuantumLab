package dev.quantumlab.desktop;

import java.io.UncheckedIOException;

/** The text a screen shows when a background computation fails. Plain Java, so it can be tested. */
final class FailureMessage {

    private FailureMessage() {}

    /** Bad input is reported as typed; a file problem and any other failure say what went wrong. */
    static String of(RuntimeException e) {
        if (e instanceof InvalidInputException) {
            return e.getMessage();
        }
        if (e instanceof UncheckedIOException io) {
            // an UncheckedIOException always has a cause: the IOException it wraps
            return "Could not write the file: " + io.getCause().getMessage();
        }
        return "Unexpected error: " + e;
    }
}
