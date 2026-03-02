package org.lakehouse.taskexecutor.api.datasource;

import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.factory.SQLTemplateFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.datasource.execute.ExecuteUtils;
import org.lakehouse.taskexecutor.api.datasource.execute.jdbc.JdbcExecuteUtils;

import java.io.IOException;

public class DataSourceManipulatorFactory {

    public static DataSourceManipulator buildDataSourceManipulator(
            DriverDTO driverDTO,
            DataSourceDTO dataSourceDTO,
            DataSetDTO dataSetDTO,
            JinJavaUtils jinJavaUtils) throws IOException {
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
                sqlTemplateDTO,
                dataSetDTO);

        return new  JdbcDataSourceManipulator(parameter);
    }

}

