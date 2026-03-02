package org.lakehouse.taskexecutor.spark.dq.runner.integrity;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.client.api.dto.configs.dataset.ColumnDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;

import java.util.Map;

public interface Check {
    Dataset<Row> nullableColumn(ColumnDTO columnDTO);
    Dataset<Row> getPrimary(Map.Entry<String, DataSetConstraintDTO> constraint);
    Dataset<Row> getForeign(Map.Entry<String, DataSetConstraintDTO> constraint);
    Dataset<Row> getUnique(Map.Entry<String, DataSetConstraintDTO> constraint);
    Dataset<Row> getCheck(Map.Entry<String, DataSetConstraintDTO> constraint);
}
