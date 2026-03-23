package org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.parameter;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorParameter;
import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.execute.SparkExecuteUtils;

public interface SparkSQLDataSourceManipulatorParameter extends DataSourceManipulatorParameter {
    SparkSession sparkSession();
    SparkExecuteUtils executeUtils();
}
