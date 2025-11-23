package org.lakehouse.taskexecutor.executionmodule;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SparkLauncherTaskProcessor extends AbstractSparkDeployTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public SparkLauncherTaskProcessor(
            TaskProcessorConfigDTO taskProcessorConfigDTO,
            RestClient.Builder builder) {
        super(taskProcessorConfigDTO, builder);
    }

    @Override
    public void runTask() throws TaskFailedException {
        TaskProcessorConfigDTO unSparkedConfig = getTaskProcessorConfig();

        DataSourceDTO dataSourceDTO = unSparkedConfig.getDataSources().get(unSparkedConfig.getTargetDataSet().getDataSourceKeyName());
        String serverUrl = String.format(
                "http://%s:%s/%s",
                dataSourceDTO.getServices().get(0).getHost(),
                dataSourceDTO.getServices().get(0).getPort(),
                dataSourceDTO.getServices().get(0).getUrn()
        );

        Map<String,String> sparkConfMap = new HashMap<>();
        sparkConfMap.putAll(filterSparkProperties( dataSourceDTO.getProperties()));
        sparkConfMap.putAll(filterSparkProperties( getTaskProcessorConfig().getExecutionModuleArgs()));

        unSparkedConfig.setExecutionModuleArgs(extractAppConf(new HashMap<>()));


        List<String> appArgs = null;
        try {
            appArgs = List.of(ObjectMapping.asJsonString(unSparkedConfig));
        } catch (JsonProcessingException e) {
            throw new TaskFailedException(e);
        }
        deploy(
                dataSourceDTO.getProperties().get("deploy.mainClass"),
                dataSourceDTO.getProperties().get("deploy.appResource"),
                serverUrl,
                sparkConfMap,
                appArgs
        );
    }
}
