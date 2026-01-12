package org.lakehouse.taskexecutor.api.datasource;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.factory.SQLTemplateFactory;
import org.lakehouse.taskexecutor.api.datasource.execute.ExecuteUtils;
import org.lakehouse.taskexecutor.api.datasource.execute.jdbc.JdbcExecuteUtils;

import java.io.IOException;

public class DataSourceManipulatorFactory {
    public static DataSourceManipulator buildDataSourceManipulator(TaskProcessorConfigDTO taskProcessorConfigDTO, Jinjava jinjava) throws IOException {
        //still one way
        DriverDTO driverDTO = taskProcessorConfigDTO.getDrivers().get(taskProcessorConfigDTO.getTargetDataSourceDTO().getDriverKeyName());
        ExecuteUtils jdbcUtils = new JdbcExecuteUtils(
                jinjava,
                taskProcessorConfigDTO.getTargetDataSourceDTO(),
                driverDTO);

        SQLTemplateDTO sqlTemplateDTO = SQLTemplateFactory.mergeSqlTemplate(
                driverDTO,
                taskProcessorConfigDTO.getTargetDataSourceDTO(),
                taskProcessorConfigDTO.getTargetDataSet());

        DataSourceManipulatorParameter parameter = new DataSourceManipulatorParameterImpl(
                jdbcUtils,
                sqlTemplateDTO,
                taskProcessorConfigDTO.getTargetDataSet());

        return new JdbcDataSourceManipulator(parameter);
    }

}

