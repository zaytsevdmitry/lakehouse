package org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.parameter;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.execute.SparkExecuteUtils;

public record SparkSQLDataSourceManipulatorParameterImpl(
        SparkSession sparkSession,
        SparkExecuteUtils executeUtils,
        SQLTemplateDTO sqlTemplateDTO,
        DataSetDTO dataSetDTO)
        implements SparkSQLDataSourceManipulatorParameter {
}
