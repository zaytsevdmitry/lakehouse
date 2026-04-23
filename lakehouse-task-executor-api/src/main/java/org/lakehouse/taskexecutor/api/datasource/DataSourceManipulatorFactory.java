package org.lakehouse.taskexecutor.api.datasource;

import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;

public interface DataSourceManipulatorFactory {
     DataSourceManipulator buildDataSourceManipulator(
             DriverDTO targetDriver,
             DataSourceDTO targetDataSource,
             DataSetDTO targetDataSet,
             JinJavaUtils jinJavaUtils,
             ConfigRestClientApi configRestClientApi)
             throws TaskConfigurationException;
}
