package org.lakehouse.taskexecutor.api.datasource.exception;

public class ReadException extends Exception {
    public ReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
