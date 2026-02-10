package org.lakehouse.taskexecutor.processor.spark;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.lakehouse.client.api.constant.TaskProcessorArgKey;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.api.utils.SparkConfUtil;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;

import java.util.List;

public class SparkLauncherTaskProcessorDQ extends AbstractSparkDeployTaskProcessor {

    private final ConfigRestClientApi configRestClientApi;
    public SparkLauncherTaskProcessorDQ(
            SourceConfDTO sourceConfDTO,
            ConfigRestClientApi configRestClientApi) {
        this.configRestClientApi = configRestClientApi;
    }

    @Override
    public void runTask(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils) throws TaskFailedException, TaskConfigurationException {
      /*  TaskProcessorConfigDTO unSparkedConfig = SparkConfUtil.unSparkConf(getScheduledTaskLockDTO());
        String dqConf = unSparkedConfig
                .getTaskProcessorArgs()
                .get(TaskProcessorArgKey.QUALITY_METRICS_CONF_KEY_NAME);
        DataSourceDTO dataSourceDTO = unSparkedConfig.getTargetDataSourceDTO();
        try {
            deploy(
                    dataSourceDTO.getService().getProperties().get("deploy.mainClass"),
                    dataSourceDTO.getService().getProperties().get("deploy.appResource"),
                    getServerUrl(dataSourceDTO.getService()),
                    SparkConfUtil.extractSparkConFromTaskConf(getScheduledTaskLockDTO()),
                    List.of(
                            ObjectMapping.asJsonString(unSparkedConfig),
                            ObjectMapping.asJsonString(qualityMetricsConfDTO)
                    )
            );
        } catch (JsonProcessingException e) {
            throw new TaskConfigurationException(e);
        }
*/    }
}
