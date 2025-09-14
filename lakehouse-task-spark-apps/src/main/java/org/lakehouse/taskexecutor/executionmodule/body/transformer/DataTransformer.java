package org.lakehouse.taskexecutor.executionmodule.body.transformer;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

public interface DataTransformer {
    Dataset<Row> transform();
}
