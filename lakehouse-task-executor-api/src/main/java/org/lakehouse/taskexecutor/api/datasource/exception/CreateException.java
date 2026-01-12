package org.lakehouse.taskexecutor.api.datasource.exception;

public class CreateException extends Exception {
    public CreateException(String message, Throwable cause) {
        super(message, cause);
    }
    public CreateException( Throwable cause) {
        super(cause);
    }
    public CreateException(String message) {
        super(message);
    }
}
