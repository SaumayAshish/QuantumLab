package dev.quantumlab.desktop;

/** Thrown when text typed by the user cannot be turned into a valid experiment setting. */
public final class InvalidInputException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }
}
