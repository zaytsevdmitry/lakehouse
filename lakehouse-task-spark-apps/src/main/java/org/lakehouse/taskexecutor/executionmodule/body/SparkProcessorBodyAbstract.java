package org.lakehouse.taskexecutor.executionmodule.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;

import java.io.IOException;

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
