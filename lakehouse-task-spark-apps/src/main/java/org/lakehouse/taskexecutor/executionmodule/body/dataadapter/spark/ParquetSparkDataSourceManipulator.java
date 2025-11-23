package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Configuration;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.*;

import java.util.List;
import java.util.Map;

public  class ParquetSparkDataSourceManipulator extends FileSparkDataSourceManipulatorAbstract {

    public static boolean isCompatible(
            Types.EngineType type,
            Types.Engine serviceType){
        return type.equals(Types.EngineType.spark);
    }
    public ParquetSparkDataSourceManipulator(SparkSession sparkSession, DataSourceDTO dataSourceDTO) {
        super(sparkSession, "parquet", dataSourceDTO);
    }


    @Override
    public void drop(String location, Map<String, String> options) throws DropException {

    }

    @Override
    public void dropPartitions(String location, List<String> partitions, Map<String, String> options) throws DropException {

    }
}
