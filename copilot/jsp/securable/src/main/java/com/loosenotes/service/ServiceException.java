package com.loosenotes.service;

/** Domain exception for service-layer business rule violations. */
public class ServiceException extends RuntimeException {

    private final ErrorCode code;

    public ServiceException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }

    public enum ErrorCode {
        DUPLICATE_USERNAME,
        DUPLICATE_EMAIL,
        INVALID_CREDENTIALS,
        NOT_FOUND,
        UNAUTHORIZED,
        VALIDATION_ERROR,
        STORAGE_ERROR
    }
}
