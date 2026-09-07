package com.aiassistiveglasses.exception;

/**
 * Thrown for 403 cases - the caller is authenticated but not allowed to
 * perform the requested action (distinct from ResourceNotFoundException,
 * which is used when the resource doesn't exist at all).
 */
public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
