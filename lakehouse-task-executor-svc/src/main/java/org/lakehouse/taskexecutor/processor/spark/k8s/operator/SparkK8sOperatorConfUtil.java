package org.lakehouse.taskexecutor.processor.spark.k8s.operator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.TaskProcessorArgKey;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.api.utils.conf.ConfUtil;
import org.lakehouse.client.api.utils.conf.SparkConfUtil;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.processor.spark.k8s.operator.entity.K8sSparkApplicationConf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SparkK8sOperatorConfUtil extends ConfUtil{
    public static Map<String,String> extractK8sOperatorConf(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO
            ){

        return Coalesce.applyMergeNonNullValuesMap(
                extractConf(sourceConfDTO.getTargetDataSource().getService().getProperties(), TaskProcessorArgKey.K8S_SPARK_OPERATOR),
                extractConf(scheduledTaskDTO.getTaskProcessorArgs(), TaskProcessorArgKey.K8S_SPARK_OPERATOR));

    }

    public static String extractMasterUrl(
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

        return url;
    }

    public static String extractAppConfJson(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO
    ) throws TaskConfigurationException {
        try {
            Map<String, String> manifestConf = extractConf(
                    extractK8sOperatorConf(
                            sourceConfDTO,
                            scheduledTaskDTO),
                    TaskProcessorArgKey
                            .K8S_SPARK_OPERATOR_MANIFEST);

            ConfigRenderOptions options = ConfigRenderOptions.defaults()
                .setJson(true)
                .setOriginComments(false)
                .setComments(false)
                .setFormatted(true);



            K8sSparkApplicationConf k8SSparkApplicationConf =  ObjectMapping
                    .stringToObject(
                            ConfigFactory
                                    .parseMap(manifestConf)
                                    .root()
                                    .render(options),
                            K8sSparkApplicationConf.class);

            String fixedTaskName;
            //todo next time need create getTaskFullName function in jijava  "k8s.spark-operator.metadata.name" : {{ k8s_dns_rfc_1123(scheduledTask)}}
            if (k8SSparkApplicationConf.getMetadata().getName()==null ||
                    k8SSparkApplicationConf.getMetadata().getName().isBlank())
                fixedTaskName = String.format(
                        "task-%d-%d",
                        scheduledTaskDTO.getId(),
                        scheduledTaskDTO.getTryNum());
            else
                fixedTaskName = k8SSparkApplicationConf.getMetadata().getName().substring(0,63);

            k8SSparkApplicationConf
                    .getMetadata()
                        .setName(fixedTaskName);

            k8SSparkApplicationConf
                    .getSpec()
                    .setSparkConf(
                            SparkConfUtil
                                    .extractSparkConFromTaskConf(
                                            sourceConfDTO,
                                            scheduledTaskDTO));


            k8SSparkApplicationConf.getSpec().setArguments(extractArguments(sourceConfDTO,scheduledTaskDTO));
            JinJavaUtils jinJavaUtils = new JinJavaFactory().getJinJavaUtils(sourceConfDTO,scheduledTaskDTO);
            String template = ObjectMapping.asJsonString(k8SSparkApplicationConf)
                .replace("\"sparkConf\" : null,","\"sparkConf\": {\n {{ sparkConf }}\n},");
            return jinJavaUtils.render(template);
        } catch (IOException e) {
            throw new TaskConfigurationException(e);
        }

    }

    private static String[] extractArguments(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO
    ) throws TaskConfigurationException {
        List<String> resultList = new ArrayList<>();
        try {
            ScheduledTaskDTO newScheduledTaskDTO =  ObjectMapping.mapToObject(
                            ObjectMapping.asMap(scheduledTaskDTO),
                            ScheduledTaskDTO.class) ;
            // task args will be sent as args outside. Clear now
            newScheduledTaskDTO.setTaskProcessorArgs(new HashMap<>());
            resultList.add(ObjectMapping.asJsonString(newScheduledTaskDTO));
        } catch (IOException e) {
            throw new TaskConfigurationException(e);
        }
        // task args outside
        resultList.addAll(
            Coalesce.applyMergeNonNullValuesMap(
                sourceConfDTO.getTargetDataSource().getService().getProperties(),
                scheduledTaskDTO.getTaskProcessorArgs())
                .entrySet()
                .stream()
                .filter(e -> !e.getKey().startsWith(TaskProcessorArgKey.K8S_SPARK_OPERATOR))
                .filter(e -> !e.getKey().startsWith(TaskProcessorArgKey.SPARK_PREFIX))
                .map(e-> String.format("--%s=%s",e.getKey(),e.getValue()))
                .toList());
        return resultList.toArray(String[]::new);
    }
}
