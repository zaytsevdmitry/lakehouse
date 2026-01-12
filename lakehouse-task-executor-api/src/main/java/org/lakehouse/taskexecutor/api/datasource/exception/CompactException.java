package org.lakehouse.taskexecutor.api.datasource.exception;

public class CompactException extends Exception {
    public CompactException(String message, Throwable cause) {
        super(message, cause);
    }
    public CompactException(String message) {
        super(message);
    }

    public CompactException(Throwable cause) {
        super(cause);
    }
}
