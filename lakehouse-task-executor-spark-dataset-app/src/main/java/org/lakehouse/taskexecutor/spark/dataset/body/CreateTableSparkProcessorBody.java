package org.lakehouse.taskexecutor.spark.dataset.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.task.executor.spark.api.configuration.SparkSessionConfiguration;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.api.datasource.exception.CreateException;
import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.body.SparkProcessorBodyAbstract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

@Service
@Import(SparkSessionConfiguration.class)
public class CreateTableSparkProcessorBody extends SparkProcessorBodyAbstract {
    private final  Logger logger = LoggerFactory.getLogger(this.getClass());

    public CreateTableSparkProcessorBody(
            ConfigRestClientApi configRestClientApi,
            SparkSession sparkSession,
            DataSourceManipulatorFactory dataSourceManipulatorFactory) {
        super(configRestClientApi,sparkSession,dataSourceManipulatorFactory);
    }


    @Override
    public void run(ScheduledTaskDTO scheduledTaskDTO) throws TaskFailedException, TaskConfigurationException {
        DataSourceManipulator targetDataSourceManipulator = getTargetDataSourceManipulator(scheduledTaskDTO);
        try {
            targetDataSourceManipulator
                    .createTableIfNotExists();
        } catch (CreateException ue){
            throw  new TaskFailedException(ue);
        }
    }


}
