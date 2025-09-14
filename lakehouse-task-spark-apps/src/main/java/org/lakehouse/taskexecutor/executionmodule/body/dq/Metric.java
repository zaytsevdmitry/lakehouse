package org.lakehouse.taskexecutor.executionmodule.body.dq;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.client.api.exception.TaskFailedException;

public interface Metric {
    public  Dataset<Row> calculate() throws TaskFailedException;
}
