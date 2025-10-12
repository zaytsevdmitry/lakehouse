package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception;

public class ConstraintException extends Exception {
    public ConstraintException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConstraintException(Throwable cause) {
        super(cause);
    }
}
