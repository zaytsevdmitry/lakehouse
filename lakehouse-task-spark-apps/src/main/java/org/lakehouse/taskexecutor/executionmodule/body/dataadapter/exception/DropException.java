package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception;

public class DropException extends Exception {
    public DropException(String message, Throwable cause) {
        super(message, cause);
    }

    public DropException(Throwable cause) {
        super(cause);
    }
}
