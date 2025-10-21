package org.lakehouse.taskexecutor.executionmodule.body.entity;

import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulator;

public class DataSetItem {
    private DataSetDTO dataSetDTO;
    private DataSourceManipulator dataSourceManipulator;

    public DataSetItem() {
    }

    public DataSetDTO getDataSetDTO() {
        return dataSetDTO;
    }

    public void setDataSetDTO(DataSetDTO dataSetDTO) {
        this.dataSetDTO = dataSetDTO;
    }

    public DataSourceManipulator getDataSourceManipulator() {
        return dataSourceManipulator;
    }

    public void setDataSourceManipulator(DataSourceManipulator dataSourceManipulator) {
        this.dataSourceManipulator = dataSourceManipulator;
    }
}
