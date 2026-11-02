package dev.quantumlab.service;

/** The caller sent input the service will not accept. Becomes HTTP 400 with the message. */
public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BadRequestException(String message) {
        super(message);
    }
}
