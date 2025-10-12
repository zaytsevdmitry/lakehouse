package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;

public abstract class ParquetSparkDataStoreManipulator extends SparkDataStoreManipulator {


    public ParquetSparkDataStoreManipulator(SparkSession sparkSession, String format, DataSourceDTO dataSourceDTO) {
        super(sparkSession, format, dataSourceDTO);
    }

}
