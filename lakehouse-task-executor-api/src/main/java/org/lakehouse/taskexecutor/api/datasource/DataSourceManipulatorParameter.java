package org.lakehouse.taskexecutor.api.datasource;

import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.taskexecutor.api.datasource.execute.ExecuteUtils;

public interface DataSourceManipulatorParameter {
    ExecuteUtils executeUtils();
    SQLTemplateDTO sqlTemplateDTO();
    DataSetDTO dataSetDTO();
}