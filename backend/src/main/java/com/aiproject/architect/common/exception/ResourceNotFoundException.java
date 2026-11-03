package com.aiproject.architect.common.exception;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String resource, String id) {
        super(String.format("%s not found with identifier: %s", resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
