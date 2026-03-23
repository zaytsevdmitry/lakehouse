package org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator;

import org.lakehouse.client.api.exception.TaskConfigurationException;

import java.io.Serial;

public class UnsuportedDataSourceException extends TaskConfigurationException {
    @Serial
    private static final long serialVersionUID = 5992353188807604489L;

    public UnsuportedDataSourceException(String message) {
        super(message);
    }
}
