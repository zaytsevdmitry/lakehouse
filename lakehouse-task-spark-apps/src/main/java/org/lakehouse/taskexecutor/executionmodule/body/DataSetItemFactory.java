package org.lakehouse.taskexecutor.executionmodule.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataStoreManipulatorFactory;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.UnsuportedDataSourceException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.ReadException;
import org.lakehouse.taskexecutor.executionmodule.body.entity.DataSetItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataSetItemFactory {
    private final DataStoreManipulatorFactory dataStoreManipulatorFactory;
    private final SparkSession sparkSession;
    public DataSetItemFactory(DataStoreManipulatorFactory dataStoreManipulatorFactory, SparkSession sparkSession) {
        this.dataStoreManipulatorFactory = dataStoreManipulatorFactory;
        this.sparkSession = sparkSession;
    }

    public DataSetItem buildDataSetItem(DataSetDTO dataSetDTO, Map<String,DataSourceDTO> dataSourceDTOs) throws ReadException, UnsuportedDataSourceException {
        DataSourceDTO dataSourceDTO = dataSourceDTOs.get(dataSetDTO.getDataSourceKeyName());
        return buildDataSetItem(dataSetDTO, dataSourceDTO);
    }

    public DataSetItem buildDataSetItem(DataSetDTO dataSetDTO, DataSourceDTO dataSourceDTO) throws UnsuportedDataSourceException, ReadException {
        DataSetItem result = new DataSetItem();
        result.setDataSetDTO( dataSetDTO);
        DataSourceManipulator dataSourceManipulator = dataStoreManipulatorFactory.buildDataStoreManipulator(dataSourceDTO,sparkSession);
        result.setDataSourceManipulator(dataSourceManipulator);
        return result;
    }
    public List<DataSetItem> buildDataSetItems(TaskProcessorConfigDTO taskProcessorConfigDTO)
            throws UnsuportedDataSourceException,
            ReadException {
        List<DataSetItem> result = new ArrayList<>();
        for ( DataSetDTO dataSetDTO: taskProcessorConfigDTO.getDataSetDTOSet()) {

            result.add(buildDataSetItem(dataSetDTO, taskProcessorConfigDTO.getDataSources()));
        }


        return result;

    }

}
