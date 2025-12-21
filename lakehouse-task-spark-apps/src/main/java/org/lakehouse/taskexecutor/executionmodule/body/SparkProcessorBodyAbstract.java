package org.lakehouse.taskexecutor.executionmodule.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulatorFactory;

public abstract class SparkProcessorBodyAbstract implements SparkProcessorBody {
    private final BodyParam bodyParam;

    public SparkProcessorBodyAbstract(BodyParam bodyParam) {
        this.bodyParam = bodyParam;

    }

    @Override
    public SparkSession getSparkSession() {
        return bodyParam.getSparkSession();
    }

    @Override
    public BodyParam getBodyParam() {
        return bodyParam;
    }

}
