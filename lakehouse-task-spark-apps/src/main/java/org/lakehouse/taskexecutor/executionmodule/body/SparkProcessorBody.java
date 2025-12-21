package org.lakehouse.taskexecutor.executionmodule.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulatorFactory;

public interface SparkProcessorBody {
    void run() throws TaskFailedException;


    SparkSession getSparkSession();

    BodyParam getBodyParam();

}

