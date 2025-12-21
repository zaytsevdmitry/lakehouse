package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.factory.TableDialectFactory;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.postgres.PostgresJdbcSparkDataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.trino.iceberg.TrinoIcebergSparkDataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.spark.IcebergSparkDataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.spark.ParquetSparkDataSourceManipulator;

import java.util.HashMap;
import java.util.Map;

public class DataSourceManipulatorFactory {
    private final TableDialectFactory tableDialectFactory = new TableDialectFactory();

    public DataSourceManipulator buildTargetDataSourceManipulator(
            SparkSession sparkSession,
            TaskProcessorConfigDTO taskProcessorConfigDTO
    ) throws TaskFailedException, UnsuportedDataSourceException {

        DataSourceDTO dataSourceDTO = taskProcessorConfigDTO.getTargetDataSourceDTO();
        DataSourceManipulatorParameter targetDataSourceManipulatorParameter = new DataSourceManipulatorParameterImpl(
                sparkSession,
                dataSourceDTO,
                taskProcessorConfigDTO.getTargetDataSet(),
                tableDialectFactory.buildTableDialect(
                        dataSourceDTO,
                        taskProcessorConfigDTO.getTargetDataSet(),
                        taskProcessorConfigDTO.getForeignDataSetDTOMap()),
                taskProcessorConfigDTO.getKeyBind());
        return buildDataSourceManipulator(targetDataSourceManipulatorParameter);
    }
    public DataSourceManipulator buildDataSourceManipulator(
            DataSourceManipulatorParameter dataSourceManipulatorParameter)
            throws UnsuportedDataSourceException {
        DataSourceManipulator result = null;
        if (dataSourceManipulatorParameter.getDataSourceDTO().getEngineType().equals(Types.EngineType.database)){
            if (dataSourceManipulatorParameter.getDataSourceDTO().getEngine().equals(Types.Engine.postgres)){
                result = new PostgresJdbcSparkDataSourceManipulator(dataSourceManipulatorParameter);
            }else if (dataSourceManipulatorParameter.getDataSourceDTO().getEngine().equals(Types.Engine.trino)){
                result =  new TrinoIcebergSparkDataSourceManipulator(dataSourceManipulatorParameter);
            }
            else {
                throw new UnsuportedDataSourceException(
                        String.format(
                                "DataSource %s unsupported. Wrong engine %s",
                                dataSourceManipulatorParameter.getDataSourceDTO().getKeyName(),
                                dataSourceManipulatorParameter.getDataSourceDTO().getEngine()

                        ));
            }
        } else if (dataSourceManipulatorParameter.getDataSourceDTO().getEngineType().equals(Types.EngineType.spark)){
            if (dataSourceManipulatorParameter.getDataSourceDTO().getEngine().equals(Types.Engine.iceberg)){
                result = new IcebergSparkDataSourceManipulator(dataSourceManipulatorParameter);
            } else if (dataSourceManipulatorParameter.getDataSourceDTO().getEngine().equals(Types.Engine.parquet)) {
                result = new ParquetSparkDataSourceManipulator(dataSourceManipulatorParameter);
            }
        } else {
                throw new UnsuportedDataSourceException(
                        String.format(
                                "DataSource %s unsupported. Wrong engineType %s",
                                dataSourceManipulatorParameter.getDataSourceDTO().getKeyName(),
                                dataSourceManipulatorParameter.getDataSourceDTO().getEngineType()
                        ));
        }

        if ( result == null ){
            throw new UnsuportedDataSourceException(
                    String.format(
                            "Wrong combination of DataSource %s. EngineType %s Engine %s",
                            dataSourceManipulatorParameter.getDataSourceDTO().getKeyName(),
                            dataSourceManipulatorParameter.getDataSourceDTO().getEngineType(),
                            dataSourceManipulatorParameter.getDataSourceDTO().getEngine()
                    ));
        }

        return result;
    }
    public Map<String,DataSourceManipulator> buildDataSourceManipulators(
            SparkSession sparkSession,
            TaskProcessorConfigDTO taskProcessorConfigDTO)
            throws UnsuportedDataSourceException, TaskFailedException {
        Map<String,DataSourceManipulator> result = new HashMap<>();
        for (DataSetDTO dataSetDTO:taskProcessorConfigDTO.getDataSetDTOs().values()) {
            DataSourceDTO dataSourceDTO = taskProcessorConfigDTO.getDataSources().get(dataSetDTO.getDataSourceKeyName());
            DataSourceManipulatorParameter p = new DataSourceManipulatorParameterImpl(
                    sparkSession,
                    dataSourceDTO,
                    dataSetDTO,
                    tableDialectFactory.buildTableDialect(
                            dataSourceDTO,
                            dataSetDTO,
                            taskProcessorConfigDTO.getForeignDataSetDTOMap()),
                    taskProcessorConfigDTO.getKeyBind());
            result.put(dataSetDTO.getKeyName(),buildDataSourceManipulator(p));
        }
        return result;
    }
}
