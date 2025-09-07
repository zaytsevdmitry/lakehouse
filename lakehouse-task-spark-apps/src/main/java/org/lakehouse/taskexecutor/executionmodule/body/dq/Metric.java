package org.lakehouse.taskexecutor.executionmodule.body.dq;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.common.api.task.processor.exception.TaskFailedException;

public interface Metric {
    public  Dataset<Row> calculate() throws TaskFailedException;
}
