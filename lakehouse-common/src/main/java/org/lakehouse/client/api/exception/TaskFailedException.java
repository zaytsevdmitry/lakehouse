package org.lakehouse.client.api.exception;

import java.io.Serial;

public class TaskFailedException extends Exception {
    @Serial
    private static final long serialVersionUID = 9052063343672506849L;

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
        super(message, cause);
    }
}
