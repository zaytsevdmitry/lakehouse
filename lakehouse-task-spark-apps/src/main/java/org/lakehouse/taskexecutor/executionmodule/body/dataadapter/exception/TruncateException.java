package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception;

public class TruncateException extends Exception {
    public TruncateException(String message, Throwable cause) {
        super(message, cause);
    }

    public TruncateException(Throwable cause) {
        super(cause);
    }
}
