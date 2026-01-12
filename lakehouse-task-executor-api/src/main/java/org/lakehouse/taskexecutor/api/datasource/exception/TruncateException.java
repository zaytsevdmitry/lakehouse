package org.lakehouse.taskexecutor.api.datasource.exception;

public class TruncateException extends Exception {
    public TruncateException(String message, Throwable cause) {
        super(message, cause);
    }

    public TruncateException(Throwable cause) {
        super(cause);
    }
}
