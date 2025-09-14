package org.lakehouse.taskexecutor.executionmodule;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.spark.SparkRestClientApi;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.configuration.SparkConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

public class SparkLauncherTaskProcessor extends AbstractSparkDeployTaskProcessor{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public SparkLauncherTaskProcessor(
            TaskProcessorConfigDTO taskProcessorConfigDTO,
            SparkConfigurationProperties sparkConfigurationProperties,
            SparkRestClientApi sparkRestClientApi) {
        super(taskProcessorConfigDTO,sparkConfigurationProperties, sparkRestClientApi);
    }

    @Override
    public void runTask() throws TaskFailedException {
        TaskProcessorConfigDTO unSparkedConfig = getTaskProcessorConfig();
        unSparkedConfig.setExecutionModuleArgs(extractAppConf(new HashMap<>()));
        List<String> appArgs = null;
        try{
            appArgs = List.of(ObjectMapping.asJsonString(unSparkedConfig));
        } catch (JsonProcessingException e) {
            throw new TaskFailedException(e);
        }
        deploy(
                "org.lakehouse.taskexecutor.executionmodule.body.SparkProcessorBodyStarter",
                "/home/dm/projects/my/lakehouse/lakehouse-task-spark-apps/target/lakehouse-task-spark-apps-0.3.0.jar",
                extractConfSpark(new HashMap<>()),
                appArgs
        );
    }
}
