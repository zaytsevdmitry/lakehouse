package org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.execute;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.taskexecutor.api.datasource.exception.ExecuteException;
import org.lakehouse.taskexecutor.api.datasource.execute.ExecuteUtils;

import java.util.Map;

public interface SparkExecuteUtils extends ExecuteUtils {
    Dataset<Row> executeQuery(String sql)throws ExecuteException;
    Dataset<Row> executeQuery(String sql, Map<String,Object> localContext)throws ExecuteException ;
}
