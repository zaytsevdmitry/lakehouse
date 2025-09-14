package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.Map;

public interface DataAdapter {
    Dataset<Row> read(String location, Map<String,String> options);
    void write(Dataset<Row> dataset,String location, Map<String,String> options);
    void clear(String location);
}
