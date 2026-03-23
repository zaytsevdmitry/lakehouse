package org.lakehouse.taskexecutor.processor.spark;

            import com.fasterxml.jackson.core.JsonProcessingException;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.api.utils.SparkConfUtil;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;

public class SparkLauncherTaskProcessorDQ extends AbstractSparkDeployTaskProcessor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SparkRestDeployFactory sparkRestDeployFactory;
    public SparkLauncherTaskProcessorDQ(SparkRestDeployFactory sparkRestDeployFactory) {
        this.sparkRestDeployFactory = sparkRestDeployFactory;
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
        try {
            deploy(
                    scheduledTaskDTO.getTaskFullName(),
                    mainClass,
                    appResource,
                    sparkRestDeployFactory.getServerUrl(sourceConfDTO,scheduledTaskDTO,jinJavaUtils),
                    SparkConfUtil.extractSparkConFromTaskConf(scheduledTaskDTO, new HashSet<>(sourceConfDTO.getDataSources().values())),
                    List.of(ObjectMapping.asJsonString(unSparkedTaskConfig)));
        } catch (JsonProcessingException e) {
            throw new TaskConfigurationException(e);
        }
    }
}
