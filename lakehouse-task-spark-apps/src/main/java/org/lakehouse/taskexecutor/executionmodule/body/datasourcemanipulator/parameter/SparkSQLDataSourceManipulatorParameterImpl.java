package org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.parameter;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.execute.SparkExecuteUtils;

public record SparkSQLDataSourceManipulatorParameterImpl(
        SparkSession sparkSession,
        SparkExecuteUtils executeUtils,
        SQLTemplateDTO sqlTemplateDTO,
        DataSetDTO dataSetDTO)
        implements SparkSQLDataSourceManipulatorParameter {
}
