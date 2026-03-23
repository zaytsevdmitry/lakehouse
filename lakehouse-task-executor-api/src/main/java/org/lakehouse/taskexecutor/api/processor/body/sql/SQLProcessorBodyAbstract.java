package org.lakehouse.taskexecutor.api.processor.body.sql;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBody;

public abstract class SQLProcessorBodyAbstract implements ProcessorBody{
    private final ConfigRestClientApi configRestClientApi;
    private final DataSourceManipulatorFactory dataSourceManipulatorFactory;
    protected SQLProcessorBodyAbstract(ConfigRestClientApi configRestClientApi, DataSourceManipulatorFactory dataSourceManipulatorFactory) {
        this.configRestClientApi = configRestClientApi;
        this.dataSourceManipulatorFactory = dataSourceManipulatorFactory;
    }

    public DataSourceManipulator getTargetDataSourceManipulator(
            ScheduledTaskDTO scheduledTaskDTO)
            throws TaskConfigurationException {
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(scheduledTaskDTO.getDataSetKeyName());
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
        try {
            jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));
            jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(scheduledTaskDTO));

        } catch (JsonProcessingException e) {
            throw new TaskConfigurationException(e);
        }
        return dataSourceManipulatorFactory
                .buildDataSourceManipulator(
                        sourceConfDTO.getTargetDriver(),
                        sourceConfDTO.getTargetDataSource(),
                        sourceConfDTO.getTargetDataSet(),
                        jinJavaUtils,
                        configRestClientApi);
    }
}
