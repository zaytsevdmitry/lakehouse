package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception;

public class CompactException extends Exception {
    public CompactException(String message, Throwable cause) {
        super(message, cause);
    }

    public CompactException(Throwable cause) {
        super(cause);
    }
}
