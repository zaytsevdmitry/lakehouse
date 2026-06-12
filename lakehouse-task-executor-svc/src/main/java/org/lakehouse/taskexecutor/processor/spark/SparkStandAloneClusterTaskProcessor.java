package org.lakehouse.taskexecutor.processor.spark;


import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.client.api.utils.conf.SparkConfUtil;
import org.lakehouse.client.rest.config.ConfigRestClientConstants;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientConstants;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.processor.spark.standalonecluster.AbstractSparkDeployTaskProcessor;
import org.lakehouse.taskexecutor.processor.spark.standalonecluster.SparkRestDeployFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service(value = "sparkLauncherTaskProcessor")
public class SparkStandAloneClusterTaskProcessor extends AbstractSparkDeployTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SparkRestDeployFactory sparkRestDeployFactory;
    private final String restConfUrl;
    private final String restSchedulerUrl;

    public SparkStandAloneClusterTaskProcessor(
            SparkRestDeployFactory sparkRestDeployFactory,
            @Value("${lakehouse.client.rest.config.server.url}") String restConfUrl,
            @Value("${lakehouse.client.rest.scheduler.server.url}") String restSchedulerUrl) {
        this.sparkRestDeployFactory = sparkRestDeployFactory;
        this.restConfUrl = restConfUrl;
        this.restSchedulerUrl = restSchedulerUrl;
    }

    @Override
    public void runTask(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils) throws TaskFailedException, TaskConfigurationException {
        String targetDataSetKeyName = scheduledTaskDTO.getDataSetKeyName();

        ScheduledTaskDTO unSparkedTaskConfig = SparkConfUtil.unSparkConf(scheduledTaskDTO);

        DataSourceDTO dataSourceDTO = sourceConfDTO.getDataSourceDTOByDataSetKeyName(targetDataSetKeyName);
        String mainClass = Coalesce.apply(
                scheduledTaskDTO.getTaskProcessorArgs().get(MAIN_CLASS_KEY),
                dataSourceDTO.getService().getProperties().get(MAIN_CLASS_KEY)
        );
        String appResource = Coalesce.apply(
                scheduledTaskDTO.getTaskProcessorArgs().get(APP_RESOURCE_KEY),
                dataSourceDTO.getService().getProperties().get(APP_RESOURCE_KEY)
        );


       // try {
            //todo clean it
           // appArgs.add(ObjectMapping.asJsonStringPretty(unSparkedTaskConfig));
            Map<String,String> argsMap = new HashMap<>(unSparkedTaskConfig.getTaskProcessorArgs());
        argsMap.put("scheduledTaskId",String.valueOf(scheduledTaskDTO.getId()));
        argsMap.put(ConfigRestClientConstants.restConfKey, restConfUrl);
        argsMap.put(SchedulerRestClientConstants.restSchedulerKey, restSchedulerUrl);

        List<String> appArgs = new ArrayList<>(argsMap
                .entrySet()
                .stream()
                .map(e-> String.format("--%s=%s",e.getKey(),e.getValue()))
                .toList());

        deploy(
                scheduledTaskDTO.buildTaskFullName(),
                mainClass,
                appResource,
                sparkRestDeployFactory.getServerUrl(sourceConfDTO,scheduledTaskDTO,jinJavaUtils),
                SparkConfUtil.extractSparkConFromTaskConf(sourceConfDTO, scheduledTaskDTO),
                appArgs);
        //} catch (JsonProcessingException e) {
        //    throw new TaskConfigurationException(e);
//        }
    }
}
