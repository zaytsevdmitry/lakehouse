package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.spark;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;

public abstract class ParquetSparkDataSourceManipulatorAbstract extends SparkDataSourceManipulatorAbstract {

    public static boolean isCompatible(
            Types.EngineType type,
            Types.Engine serviceType){
        return type.equals(Types.EngineType.spark);
    }
    public ParquetSparkDataSourceManipulatorAbstract(SparkSession sparkSession, String format, DataSourceDTO dataSourceDTO) {
        super(sparkSession, format, dataSourceDTO);
    }

}
