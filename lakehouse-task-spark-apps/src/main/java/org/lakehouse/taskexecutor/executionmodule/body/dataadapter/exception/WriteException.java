package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception;

public class WriteException extends Exception {
    public WriteException(String message, Throwable cause) {
        super(message, cause);
    }

    public WriteException(Throwable cause) {
        super(cause);
    }
}
