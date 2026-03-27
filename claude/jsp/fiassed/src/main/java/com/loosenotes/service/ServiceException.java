package com.loosenotes.service;

/**
 * Checked exception for service-layer failures. Carries a machine-readable
 * {@code code} (e.g. "VALIDATION", "ACCESS_DENIED", "NOT_FOUND", "DUPLICATE")
 * so callers can distinguish failure modes without parsing message text.
 */
public class ServiceException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String code;

    /**
     * @param code    Short, stable identifier for the failure category.
     * @param message Human-readable description (safe for logs; do NOT echo raw
     *                to end-users without filtering).
     */
    public ServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * @param code    Short, stable identifier for the failure category.
     * @param message Human-readable description.
     * @param cause   Underlying cause.
     */
    public ServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /** Returns the machine-readable failure code (never {@code null}). */
    public String getCode() {
        return code;
    }
}
