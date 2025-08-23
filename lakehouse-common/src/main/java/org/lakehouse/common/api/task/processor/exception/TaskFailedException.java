package org.lakehouse.common.api.task.processor.exception;

public class TaskFailedException extends Exception{
    public TaskFailedException() {
        super();
    }

    public TaskFailedException(Throwable cause) {
        super(cause);
    }

    public TaskFailedException(String message) {
        super(message);
    }
    public TaskFailedException(String message, Throwable cause) {
        super(message,cause);
    }
}
