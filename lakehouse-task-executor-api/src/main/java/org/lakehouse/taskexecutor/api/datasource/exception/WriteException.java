package org.lakehouse.taskexecutor.api.datasource.exception;

public class WriteException extends Exception {
    public WriteException(String message, Throwable cause) {
        super(message, cause);
    }

    public WriteException(Throwable cause) {
        super(cause);
    }
}
