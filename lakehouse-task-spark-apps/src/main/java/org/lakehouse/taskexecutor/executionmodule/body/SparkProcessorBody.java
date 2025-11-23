package org.lakehouse.taskexecutor.executionmodule.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;

public interface SparkProcessorBody {
    public void run() throws TaskFailedException;

    public TaskProcessorConfigDTO getTaskProcessorConfigDTO();

    public SparkSession getSparkSession();

    public BodyParam getBodyParam();
}
