package dev.quantumlab.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns a rejected input into a plain 400 answer with a readable message. */
@RestControllerAdvice
public class ErrorHandler {

    /** The JSON body of an error answer. */
    public record ErrorBody(String error) {}

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorBody badRequest(BadRequestException e) {
        return new ErrorBody(e.getMessage());
    }
}
