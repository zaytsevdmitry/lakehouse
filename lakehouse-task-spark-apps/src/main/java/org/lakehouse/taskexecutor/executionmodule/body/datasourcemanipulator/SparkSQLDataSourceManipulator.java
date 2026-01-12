package org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.client.api.constant.Configuration;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.exception.ReadException;
import org.lakehouse.taskexecutor.api.datasource.exception.WriteException;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameter;

import java.util.Map;

public interface SparkSQLDataSourceManipulator extends DataSourceManipulator,
        SparkSQLDataSourceManipulatorParameter {
    Dataset<Row> read(Map<String,String> options)  throws ReadException;
    void write(Dataset<Row> dataset, Configuration.ModificationRule modificationRule) throws WriteException;
}