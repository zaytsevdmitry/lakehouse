package org.lakehouse.taskexecutor.processor.spark;

import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.springframework.stereotype.Service;

/**
 * Based on spark restapi
 * @apiNote  <a href="https://spark.apache.org/docs/3.5.8/spark-standalone.html#rest-api">...</a>
 * restApi version 1 (/v1/submissions)
 * */
@Service
public class SparkRestDeployFactory {

    private final String urnV1 = "/v1/submissions";

    public SparkRestDeployFactory() {}


    public String getServerUrl(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils) throws TaskConfigurationException {
        DriverDTO driverDTO = sourceConfDTO.getTargetDriver();

        if (!driverDTO.getConnectionTemplates().containsKey(Types.ConnectionType.spark))
            throw new TaskConfigurationException(String.format("Connection template %s is not present in driver %s", Types.ConnectionType.spark.label,driverDTO.getKeyName()));

        if(!scheduledTaskDTO.getTaskProcessorArgs().containsKey(SystemVarKeys.CONNECTION_STRING_PROTOCOL_NAME))
            throw new TaskConfigurationException(
                    String.format(
                            "Key '%s' is not present in TaskProcessorArgs %s",
                            SystemVarKeys.CONNECTION_STRING_PROTOCOL_NAME,
                            scheduledTaskDTO.getTaskFullName() ));


        String template = driverDTO.getConnectionTemplates().get(Types.ConnectionType.spark);
        String url = jinJavaUtils.render(template);
        return url + urnV1;
    }

}
