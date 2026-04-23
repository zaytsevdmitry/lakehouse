package org.lakehouse.taskexecutor.api.processor.body.sql;

import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.springframework.stereotype.Service;

@Service(value = "mergeSQLProcessorBody")
public class MergeSQLProcessorBody extends ScriptSQLProcessorBodyAbstract {
    private final ConfigRestClientApi configRestClientApi;
    public MergeSQLProcessorBody(
            ConfigRestClientApi configRestClientApi,
            DataSourceManipulatorFactory dataSourceManipulatorFactory) {
        super(configRestClientApi,dataSourceManipulatorFactory);
        this.configRestClientApi = configRestClientApi;
    }

    @Override
    public void run(ScheduledTaskDTO scheduledTaskDTO) throws TaskFailedException, TaskConfigurationException {
        DataSourceManipulator targetDataSourceManipulator = getTargetDataSourceManipulator(scheduledTaskDTO);
        execute(
                targetDataSourceManipulator.executeUtils(),
                targetDataSourceManipulator.sqlTemplateResolver().getMergeDML(),
                configRestClientApi.getDataSetModelScript(targetDataSourceManipulator.dataSetDTO().getKeyName())
        );
    }
}
