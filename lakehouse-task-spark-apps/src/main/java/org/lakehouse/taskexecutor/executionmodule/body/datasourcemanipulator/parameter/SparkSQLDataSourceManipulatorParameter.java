package org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.parameter;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorParameter;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.execute.SparkExecuteUtils;

public interface SparkSQLDataSourceManipulatorParameter extends DataSourceManipulatorParameter {
    SparkSession sparkSession();
    SparkExecuteUtils executeUtils();
}
