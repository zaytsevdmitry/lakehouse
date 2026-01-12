package org.lakehouse.taskexecutor.executionmodule.body.transformer;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.SparkSQLDataSourceManipulator;

import java.util.Map;

public interface DataTransformer {
    Dataset<Row> transform(
            Map<String, SparkSQLDataSourceManipulator> sourceDataSourceManipulators,
            SparkSQLDataSourceManipulator targetSparkDataSourceManipulator)
            throws TransformationException;
}
