package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception;

public class CreateException extends Exception {
    public CreateException(String message, Throwable cause) {
        super(message, cause);
    }
    public CreateException(String message) {
        super(message);
    }
}
