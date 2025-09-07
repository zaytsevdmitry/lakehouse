package org.lakehouse.taskexecutor.executionmodule;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.javatuples.Tuple;
import org.lakehouse.client.api.dto.configs.QualityMetricsConfDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.spark.SparkRestClientApi;
import org.lakehouse.client.rest.spark.standalone.CreateRequest;
import org.lakehouse.client.rest.spark.standalone.CreateResponse;
import org.lakehouse.client.rest.spark.standalone.StatusResponse;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
import org.lakehouse.common.api.task.processor.exception.TaskFailedException;
import org.lakehouse.taskexecutor.configuration.SparkConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SparkLauncherTaskProcessorDQ extends AbstractSparkDeployTaskProcessor{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final List<QualityMetricsConfDTO> qualityMetricsConfDTOS;
    public SparkLauncherTaskProcessorDQ(
            TaskProcessorConfigDTO taskProcessorConfigDTO,
            SparkConfigurationProperties sparkConfigurationProperties,
            SparkRestClientApi sparkRestClientApi,
            List<QualityMetricsConfDTO> qualityMetricsConfDTOS) {
        super(taskProcessorConfigDTO,sparkConfigurationProperties, sparkRestClientApi);
        this.qualityMetricsConfDTOS = qualityMetricsConfDTOS;
    }

    private void submit(
            String unSparkedConfigStr,
            QualityMetricsConfDTO qualityMetricsConfDTO) throws TaskFailedException {

        List<String> appArgs = null;
        try{
            appArgs = List.of(unSparkedConfigStr, ObjectMapping.asJsonString(qualityMetricsConfDTO));
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
    @Override
    public void runTask() throws TaskFailedException {

        TaskProcessorConfigDTO unSparkedConfig = getTaskProcessorConfig();
        unSparkedConfig.setExecutionModuleArgs(extractAppConf(new HashMap<>()));
        String unSparkedConfigStr = null;

        try{
            unSparkedConfigStr = ObjectMapping.asJsonString(unSparkedConfig);
        } catch (JsonProcessingException e) {
            throw new TaskFailedException(e);
        }


        String finalUnSparkedConfigStr = unSparkedConfigStr;
        for (QualityMetricsConfDTO qualityMetricsConfDTO: qualityMetricsConfDTOS) {
            submit(finalUnSparkedConfigStr,qualityMetricsConfDTO);
        };

    }

}
