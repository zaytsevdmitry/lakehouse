package org.lakehouse.taskexecutor.api.datasource.exception;

public class ExecuteException extends Exception {
    public ExecuteException(String message, Throwable cause) {
        super(message, cause);
    }
    public ExecuteException(String message) {
        super(message);
    }
    public ExecuteException(Throwable cause) {
        super(cause);
    }
}
