package org.lakehouse.taskexecutor.exception;

public class DataSetIntervalException extends Exception {
    private static final long serialVersionUID = 1464725978028001612L;

    public DataSetIntervalException() {
        super();
    }


    public DataSetIntervalException(String msg) {
        super(msg);
    }

    public DataSetIntervalException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
