package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.postgres.PostgresJdbcSparkDataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.trino.iceberg.TrinoIcebergSparkDataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.spark.IcebergSparkDataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.spark.ParquetSparkDataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.spark.SparkDataSourceManipulatorAbstract;

public class DataStoreManipulatorFactory {
    public DataSourceManipulator buildDataStoreManipulator(
            DataSourceDTO dataSourceDTO,
            SparkSession sparkSession)
            throws UnsuportedDataSourceException {
        DataSourceManipulator result = null;
        if (dataSourceDTO.getEngineType().equals(Types.EngineType.database)){
            if (dataSourceDTO.getEngine().equals(Types.Engine.postgres)){
                result = new PostgresJdbcSparkDataSourceManipulator(sparkSession,dataSourceDTO, new JdbcUtils());
            }else if (dataSourceDTO.getEngine().equals(Types.Engine.trino)){
                result =  new TrinoIcebergSparkDataSourceManipulator(sparkSession,dataSourceDTO, new JdbcUtils());
            }
            else {
                throw new UnsuportedDataSourceException(
                        String.format(
                                "DataSource %s unsupported. Wrong engine %s",
                                dataSourceDTO.getKeyName(),
                                dataSourceDTO.getEngine()

                        ));
            }
        } else if (dataSourceDTO.getEngineType().equals(Types.EngineType.spark)){
            if (dataSourceDTO.getEngine().equals(Types.Engine.iceberg)){
                result = new IcebergSparkDataSourceManipulator(sparkSession,dataSourceDTO);
            } else if (dataSourceDTO.getEngine().equals(Types.Engine.parquet)) {
                result = new ParquetSparkDataSourceManipulator(sparkSession,dataSourceDTO);
            }
        } else {
                throw new UnsuportedDataSourceException(
                        String.format(
                                "DataSource %s unsupported. Wrong engineType %s",
                                dataSourceDTO.getKeyName(),
                                dataSourceDTO.getEngineType()
                        ));
        }

        if ( result == null ){
            throw new UnsuportedDataSourceException(
                    String.format(
                            "Wrong combination of DataSource %s. EngineType %s Engine %s",
                            dataSourceDTO.getKeyName(),
                            dataSourceDTO.getEngineType(),
                            dataSourceDTO.getEngine()
                    ));
        }

        return result;
    }
}
