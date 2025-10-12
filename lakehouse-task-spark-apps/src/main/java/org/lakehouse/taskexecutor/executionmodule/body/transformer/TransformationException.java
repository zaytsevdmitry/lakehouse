package org.lakehouse.taskexecutor.executionmodule.body.transformer;

public class TransformationException extends Exception {
    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransformationException(Throwable cause) {
        super(cause);
    }
}
