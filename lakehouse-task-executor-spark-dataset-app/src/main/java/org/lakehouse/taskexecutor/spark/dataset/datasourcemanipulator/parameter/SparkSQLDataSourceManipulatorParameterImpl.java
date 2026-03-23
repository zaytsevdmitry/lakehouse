package org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.parameter;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.taskexecutor.api.facade.SQLTemplateResolver;
import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.execute.SparkExecuteUtils;

public record SparkSQLDataSourceManipulatorParameterImpl(
        SparkSession sparkSession,
        SparkExecuteUtils executeUtils,
        SQLTemplateResolver sqlTemplateResolver,
        DataSetDTO dataSetDTO)
        implements SparkSQLDataSourceManipulatorParameter {
}
