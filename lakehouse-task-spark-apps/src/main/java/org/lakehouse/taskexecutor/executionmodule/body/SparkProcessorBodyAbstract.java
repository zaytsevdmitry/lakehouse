package org.lakehouse.taskexecutor.executionmodule.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;

public abstract class SparkProcessorBodyAbstract implements SparkProcessorBody{
    private final BodyParam bodyParam;
    public SparkProcessorBodyAbstract(BodyParam bodyParam)  {
        this.bodyParam = bodyParam;

    }

    @Override
    public TaskProcessorConfigDTO getTaskProcessorConfigDTO() {
        return bodyParam.getTaskProcessorConfigDTO();
    }
    @Override
    public SparkSession getSparkSession(){
        return bodyParam.getSparkSession();
    }

    @Override
    public BodyParam getBodyParam(){
        return bodyParam;
    }
}
