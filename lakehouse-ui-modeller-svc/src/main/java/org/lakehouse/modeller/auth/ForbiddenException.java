package org.lakehouse.modeller.auth;

/**
 * Raised when a user is not allowed to perform an operation under {@code jwt-rbac}
 * (missing or insufficient LAKEHOUSE_MODELLER_* role). Maps to HTTP 403.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}