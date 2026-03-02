package org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator;

public class UnsuportedDataSourceException extends Exception {
    public UnsuportedDataSourceException(String message) {
        super(message);
    }
}
