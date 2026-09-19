package org.lakehouse.ui.modeller.auth;

/**
 * Raised when a referenced resource (workspace, file) does not exist.
 * Maps to HTTP 404.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}