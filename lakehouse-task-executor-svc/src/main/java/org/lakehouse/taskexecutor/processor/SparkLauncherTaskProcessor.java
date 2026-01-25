package org.lakehouse.taskexecutor.processor;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.api.utils.SparkConfUtil;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Based on spark restapi
 * @apiNote  https://spark.apache.org/docs/3.5.7/spark-standalone.html#rest-api
 * restApi version 1 (/v1/submissions)
 * */
public class SparkLauncherTaskProcessor extends AbstractSparkDeployTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String urnV1 = "/v1/submissions";
    public SparkLauncherTaskProcessor(
            TaskProcessorConfigDTO taskProcessorConfigDTO) {
        super(taskProcessorConfigDTO);
    }

    private Jinjava getJinJava(TaskProcessorConfigDTO taskProcessorConfigDTO) throws TaskConfigurationException {
        try {
            return  JinJavaFactory.getJinjava(getTaskProcessorConfig());
        } catch (JsonProcessingException e) {
            throw new TaskConfigurationException(e);
        }
    }

    private DriverDTO getDriverDTO(){
        return getTaskProcessorConfig().getDrivers().get(getTaskProcessorConfig().getTargetDataSourceDTO().getDriverKeyName());
    }
    private String getServerUrl(ServiceDTO serviceDTO) throws TaskConfigurationException {
        DriverDTO driverDTO = getTaskProcessorConfig().getDrivers().get(getTaskProcessorConfig().getTargetDataSourceDTO().getDriverKeyName());

        if (!driverDTO.getConnectionTemplates().containsKey(Types.ConnectionType.spark))
            throw new TaskConfigurationException(String.format("Connection template %s is not present in driver %s", Types.ConnectionType.spark.label,driverDTO.getKeyName()));

        if(!getTaskProcessorConfig().getTaskProcessorArgs().containsKey(SystemVarKeys.CONNECTION_STRING_PROTOCOL_NAME))
            throw new TaskConfigurationException(
                    String.format(
                            "Key '%s' is not present in TaskProcessorArgs %s",
                            SystemVarKeys.CONNECTION_STRING_PROTOCOL_NAME,
                            getTaskProcessorConfig().getTaskFullName() ));

        String protocol = getTaskProcessorConfig().getTaskProcessorArgs().get(SystemVarKeys.CONNECTION_STRING_PROTOCOL_NAME);

        String template = driverDTO.getConnectionTemplates().get(Types.ConnectionType.spark);
        Jinjava jinjava = getJinJava(getTaskProcessorConfig());
        try {
            Map<String,Object> localContext = Map.of(
                    SystemVarKeys.SERVICE_KEY,ObjectMapping.asMap(serviceDTO),
                    SystemVarKeys.CONNECTION_STRING_PROTOCOL_NAME, protocol
                    );
            return jinjava.render(template, localContext) + urnV1;
        } catch (JsonProcessingException e) {
            throw new TaskConfigurationException(e);
        }
    }

    @Override
    public void runTask() throws TaskFailedException, TaskConfigurationException {
        TaskProcessorConfigDTO unSparkedConfig = getTaskProcessorConfig();

        DataSourceDTO dataSourceDTO = unSparkedConfig.getTargetDataSourceDTO();

        Map<String,String> sparkConfMap = new HashMap<>();
        //first datasource level spark properties
        new HashSet<>(
                        getTaskProcessorConfig()
                                .getDataSources()
                                .values())
                        .forEach(d -> sparkConfMap.putAll(SparkConfUtil.startWithSpark(d.getService().getProperties())));
        // second task args level spark properties
        sparkConfMap.putAll(SparkConfUtil.startWithSpark( getTaskProcessorConfig().getTaskProcessorArgs()));

        // least task args
        unSparkedConfig
                .setTaskProcessorArgs(
                        SparkConfUtil
                                .extractAppConf(
                                        getTaskProcessorConfig()
                                                .getTaskProcessorArgs()));


        List<String> appArgs = null;

        try {
            appArgs = List.of(ObjectMapping.asJsonString(unSparkedConfig));
        } catch (JsonProcessingException e) {
            throw new TaskConfigurationException(e);
        }

        deploy(
                dataSourceDTO.getService().getProperties().get("deploy.mainClass"),
                dataSourceDTO.getService().getProperties().get("deploy.appResource"),
                getServerUrl(dataSourceDTO.getService()),
                sparkConfMap,
                appArgs
        );
    }
}
