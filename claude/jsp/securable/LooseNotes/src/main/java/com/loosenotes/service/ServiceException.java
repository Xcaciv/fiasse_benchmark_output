package com.loosenotes.service;

/**
 * Signals a domain-level validation or business rule violation.
 *
 * SSEM notes:
 * - Resilience: specific exception type avoids catching broad Exception classes.
 * - Analyzability: message is user-facing; stack trace is not exposed to clients.
 */
public class ServiceException extends Exception {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
