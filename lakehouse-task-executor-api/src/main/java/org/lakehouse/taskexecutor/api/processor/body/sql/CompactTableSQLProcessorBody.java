package org.lakehouse.taskexecutor.api.processor.body.sql;

import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.api.datasource.exception.CompactException;
import org.springframework.stereotype.Service;

@Service
public class CompactTableSQLProcessorBody extends ScriptSQLProcessorBodyAbstract {
    public CompactTableSQLProcessorBody(ConfigRestClientApi configRestClientApi,
                                        DataSourceManipulatorFactory dataSourceManipulatorFactory) {
        super(configRestClientApi, dataSourceManipulatorFactory);
    }



    @Override
    public void run(ScheduledTaskDTO scheduledTaskDTO) throws TaskFailedException, TaskConfigurationException {
        try {
            getTargetDataSourceManipulator(scheduledTaskDTO).compact();
        } catch (CompactException e) {
            throw new TaskFailedException(e);
        }
    }
}
