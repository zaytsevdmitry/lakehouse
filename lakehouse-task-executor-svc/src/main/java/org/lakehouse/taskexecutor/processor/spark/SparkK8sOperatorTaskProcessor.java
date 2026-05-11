package org.lakehouse.taskexecutor.processor.spark;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.TaskProcessorArgKey;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.processor.TaskProcessor;
import org.lakehouse.taskexecutor.processor.spark.k8s.operator.K8sSparkOperatorClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SparkK8sOperatorTaskProcessor implements TaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void runTask(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils)
            throws TaskFailedException,
                   TaskConfigurationException {
//todo вынести конфиги в отдельные фабрики , покрыть своими тестами  Json conf is {} java.lang.NullPointerException: Cannot invoke "io.fabric8.kubernetes.api.model.ObjectMeta.getName()" because the return value of "io.fabric8.kubernetes.api.model.GenericKubernetesResource.getMetadata()" is null

        Map<String, String> taskParams = Coalesce.applyMergeNonNullValuesMap(
            filterConf(sourceConfDTO.getTargetDataSource().getService().getProperties(), TaskProcessorArgKey.K8S_SPARK_OPERATOR),
            filterConf(scheduledTaskDTO.getTaskProcessorArgs(), TaskProcessorArgKey.K8S_SPARK_OPERATOR));

        taskParams.forEach((s, s2) -> logger.info("Task parameter {} -> {}", s,s2));

        String masterUrl =  getServerUrl(sourceConfDTO,scheduledTaskDTO,jinJavaUtils); //taskParams.get("masterUrl");
        logger.info("MasterUrl is {}", masterUrl);
        //String token = taskParams.getOrDefault("token", "");
        String namespace = taskParams.getOrDefault("metadata.namespace", "default");
        logger.info("Namespace is {}", namespace);

        K8sSparkOperatorClientUtils utils = new K8sSparkOperatorClientUtils();
        KubernetesClient kubernetesClient = utils.buildKubernetesClient(
                masterUrl,
                //token,
                namespace);

        String json = ConfigFactory
                .parseMap(filterConf(taskParams,TaskProcessorArgKey.K8S_SPARK_OPERATOR.concat("spec.")))
                .root()
                .render(ConfigRenderOptions.concise().setFormatted(true));

        utils.submit(kubernetesClient, json);
    }

    private Map<String,String> filterConf(Map<String,String> conf, String startWith){
        return conf
                .entrySet()
                .stream()
                .filter(e->e.getKey().startsWith(startWith))
                .map(e ->  Map.entry(e.getKey().replace(startWith, ""), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
    }
    private String getServerUrl(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils) throws TaskConfigurationException {
        DriverDTO driverDTO = sourceConfDTO.getTargetDriver();

        if (!driverDTO.getConnectionTemplates().containsKey(Types.ConnectionType.spark))
            throw new TaskConfigurationException(String.format("Connection template %s is not present in driver %s", Types.ConnectionType.spark.label, driverDTO.getKeyName()));

        if(!scheduledTaskDTO.getTaskProcessorArgs().containsKey(SystemVarKeys.CONNECTION_STRING_PROTOCOL_NAME))
            throw new TaskConfigurationException(
                    String.format(
                            "Key '%s' is not present in TaskProcessorArgs %s",
                            SystemVarKeys.CONNECTION_STRING_PROTOCOL_NAME,
                            scheduledTaskDTO.getTaskFullName() ));


        String template = driverDTO.getConnectionTemplates().get(Types.ConnectionType.spark);
        String url = jinJavaUtils.render(template);
        logger.info("K8s server url is {}", url);
        return url;
    }
}
