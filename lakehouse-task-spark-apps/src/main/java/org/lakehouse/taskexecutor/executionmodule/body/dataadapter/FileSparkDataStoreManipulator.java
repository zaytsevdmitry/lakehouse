package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;

public abstract class FileSparkDataStoreManipulator extends SparkDataStoreManipulator {


    public FileSparkDataStoreManipulator(SparkSession sparkSession, String format, DataSourceDTO dataSourceDTO) {
        super(sparkSession, format, dataSourceDTO);
    }

}
