package org.lakehouse.taskexecutor.executionmodule.body.transformer;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulator;


import java.util.List;
import java.util.Map;

public interface DataTransformer {
    Dataset<Row> transform(
            Map<String,DataSourceManipulator> sourceDataSourceManipulators,
            DataSourceManipulator targetDataSourceManipulator)
            throws TransformationException;
}
