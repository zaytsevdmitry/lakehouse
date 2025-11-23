package org.lakehouse.taskexecutor.executionmodule.body.transformer;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.taskexecutor.executionmodule.body.entity.DataSetItem;

import java.util.List;

public interface DataTransformer {
    Dataset<Row> transform(List<DataSetItem> sourceDataSetItems, DataSetItem targetDataSetItem) throws TransformationException;
}
