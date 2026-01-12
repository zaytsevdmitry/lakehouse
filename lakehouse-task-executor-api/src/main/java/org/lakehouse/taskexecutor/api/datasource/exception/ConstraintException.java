package org.lakehouse.taskexecutor.api.datasource.exception;

public class ConstraintException extends Exception {
    public ConstraintException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConstraintException(Throwable cause) {
        super(cause);
    }
    public ConstraintException(String message) {
        super(message);
    }
}
