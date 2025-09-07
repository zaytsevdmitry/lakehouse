package org.lakehouse.taskexecutor.executionmodule.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
import org.lakehouse.common.api.task.processor.exception.TaskFailedException;

public interface SparkProcessorBody {
    public void run() throws TaskFailedException;
    public TaskProcessorConfigDTO getTaskProcessorConfigDTO();
    public SparkSession getSparkSession();
    public BodyParam getBodyParam();
}
