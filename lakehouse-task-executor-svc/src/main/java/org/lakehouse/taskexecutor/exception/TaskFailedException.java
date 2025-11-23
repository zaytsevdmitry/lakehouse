package org.lakehouse.taskexecutor.exception;

public class TaskFailedException extends Exception {
    public TaskFailedException() {
        super();
    }

    public TaskFailedException(Throwable cause) {
        super(cause);
    }

    public TaskFailedException(String message) {
        super(message);
    }
}
