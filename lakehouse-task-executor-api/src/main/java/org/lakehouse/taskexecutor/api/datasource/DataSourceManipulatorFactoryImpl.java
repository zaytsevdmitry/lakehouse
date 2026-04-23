package org.lakehouse.taskexecutor.api.datasource;

import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.factory.SQLTemplateFactory;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.datasource.execute.ExecuteUtils;
import org.lakehouse.taskexecutor.api.datasource.execute.jdbc.JdbcExecuteUtils;
import org.lakehouse.taskexecutor.api.facade.SQLTemplateResolver;

public class DataSourceManipulatorFactoryImpl implements DataSourceManipulatorFactory{

    public  DataSourceManipulator buildDataSourceManipulator(
            DriverDTO driverDTO,
            DataSourceDTO dataSourceDTO,
            DataSetDTO dataSetDTO,
            JinJavaUtils jinJavaUtils,
            ConfigRestClientApi configRestClientApi) throws TaskConfigurationException {
        //still one way
        ExecuteUtils jdbcUtils = new JdbcExecuteUtils(
                jinJavaUtils,
                dataSourceDTO,
                driverDTO);

        SQLTemplateDTO sqlTemplateDTO = SQLTemplateFactory.mergeSqlTemplate(
                driverDTO,
                dataSourceDTO,
                dataSetDTO);

        DataSourceManipulatorParameter parameter = new DataSourceManipulatorParameterImpl(
                jdbcUtils,
                new SQLTemplateResolver(configRestClientApi,sqlTemplateDTO),
                dataSetDTO);

        return new  JdbcDataSourceManipulator(parameter);
    }

}

