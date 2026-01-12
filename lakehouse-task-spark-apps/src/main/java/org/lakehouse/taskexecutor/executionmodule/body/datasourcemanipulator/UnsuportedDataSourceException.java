package org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator;

public class UnsuportedDataSourceException extends Exception {
    public UnsuportedDataSourceException(String message) {
        super(message);
    }
}
