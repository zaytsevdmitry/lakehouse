package org.lakehouse.modeller.yaml;

/**
 * Raised when a YAML metadata document cannot be parsed, serialized or located.
 * Maps to HTTP 400 by {@code GlobalExceptionHandler}.
 */
public class VcsConfigParseException extends RuntimeException {

    public VcsConfigParseException(String message) {
        super(message);
    }

    public VcsConfigParseException(String message, Throwable cause) {
        super(message, cause);
    }
}