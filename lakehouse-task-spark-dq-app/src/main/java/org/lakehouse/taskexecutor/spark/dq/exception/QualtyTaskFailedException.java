package org.lakehouse.taskexecutor.spark.dq.exception;

public class QualtyTaskFailedException extends Exception {
    public QualtyTaskFailedException() {
        super();
    }

    public QualtyTaskFailedException(Throwable cause) {
        super(cause);
    }

    public QualtyTaskFailedException(String message) {
        super(message);
    }

    public QualtyTaskFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
