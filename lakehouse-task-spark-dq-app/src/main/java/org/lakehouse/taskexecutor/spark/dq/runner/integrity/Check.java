package org.lakehouse.taskexecutor.spark.dq.runner.integrity;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.client.api.dto.configs.dataset.ColumnDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;

import java.util.Map;

public interface Check {
    Dataset<Row> nullableColumn(ColumnDTO columnDTO);
    Dataset<Row> getPrimary(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException;
    Dataset<Row> getForeign(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException;
    Dataset<Row> getUnique(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException;
    Dataset<Row> getCheck(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException;
}
