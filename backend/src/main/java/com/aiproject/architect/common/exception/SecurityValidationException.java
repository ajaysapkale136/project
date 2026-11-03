package com.aiproject.architect.common.exception;

public class SecurityValidationException extends AppException {
    public SecurityValidationException(String message) {
        super(message);
    }

    public SecurityValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
