package org.lakehouse.taskexecutor.api.datasource;

import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.taskexecutor.api.datasource.execute.ExecuteUtils;
import org.lakehouse.taskexecutor.api.facade.SQLTemplateResolver;

public interface DataSourceManipulatorParameter {
    ExecuteUtils executeUtils();
    SQLTemplateResolver sqlTemplateResolver();
    DataSetDTO dataSetDTO();
}