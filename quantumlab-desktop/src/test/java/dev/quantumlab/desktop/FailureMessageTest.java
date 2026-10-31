package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FailureMessageTest {

    @Test
    @DisplayName("bad input is shown exactly as the message says")
    void invalidInput() {
        assertEquals("Trials must be a whole number.", FailureMessage.of(new InvalidInputException("Trials must be a whole number.")));
    }

    @Test
    @DisplayName("a file problem names the cause, not the wrapper")
    void fileProblem() {
        var e = new UncheckedIOException("wrapper", new IOException("disk full"));
        assertEquals("Could not write the file: disk full", FailureMessage.of(e));
    }

    @Test
    @DisplayName("anything else is reported as unexpected, with its type")
    void unexpected() {
        assertEquals("Unexpected error: java.lang.IllegalStateException: boom", FailureMessage.of(new IllegalStateException("boom")));
        assertEquals("Unexpected error: java.lang.NullPointerException", FailureMessage.of(new NullPointerException()));
    }

    @Test
    @DisplayName("input errors are not mistaken for unexpected ones")
    void order() {
        // InvalidInputException is an IllegalArgumentException: it must still take the first branch
        String m = FailureMessage.of(new InvalidInputException("x"));
        assertEquals("x", m);
    }
}
