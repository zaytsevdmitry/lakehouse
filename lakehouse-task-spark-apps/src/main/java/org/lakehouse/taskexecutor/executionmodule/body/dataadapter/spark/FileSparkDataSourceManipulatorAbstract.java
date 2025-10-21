package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.spark;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;

public abstract class FileSparkDataSourceManipulatorAbstract extends SparkDataSourceManipulatorAbstract {


    public FileSparkDataSourceManipulatorAbstract(SparkSession sparkSession, String format, DataSourceDTO dataSourceDTO) {
        super(sparkSession, format, dataSourceDTO);
    }

}
