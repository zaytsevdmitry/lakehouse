package org.lakehouse.taskexecutor.executionmodule.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;

public abstract class SparkProcessorBodyAbstract implements SparkProcessorBody{
    private final TaskProcessorConfigDTO taskProcessorConfigDTO;
    private final SparkSession sparkSession;
    public SparkProcessorBodyAbstract(
            SparkSession sparkSession,
            TaskProcessorConfigDTO taskProcessorConfigDTO) {
        this.sparkSession = sparkSession;
        this.taskProcessorConfigDTO = taskProcessorConfigDTO;
    }

    @Override
    public TaskProcessorConfigDTO getTaskProcessorConfigDTO() {
        return taskProcessorConfigDTO;
    }
    @Override
    public SparkSession getSparkSession(){
        return sparkSession;
    }
}
