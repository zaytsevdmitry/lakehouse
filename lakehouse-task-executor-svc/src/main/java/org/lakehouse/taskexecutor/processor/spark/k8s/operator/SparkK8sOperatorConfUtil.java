package org.lakehouse.taskexecutor.processor.spark.k8s.operator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValueFactory;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.TaskProcessorArgKey;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.api.utils.conf.SparkConfUtil;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.processor.spark.k8s.operator.entity.K8sSparkApplicationConf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SparkK8sOperatorConfUtil {
    private static Map<String,Object> extractConf(Map<String,String> conf, String startWith){
        return conf
                .entrySet()
                .stream()
                .filter(e->e.getKey().startsWith(startWith))
                .map(e ->  Map.entry(e.getKey().replace(startWith, ""), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
    }
    public static Map<String,Object> extractK8sOperatorConf(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO
            ){

        return Coalesce.applyMergeNonNullValuesMap(
                extractConf(sourceConfDTO.getTargetDataSource().getService().getProperties(), TaskProcessorArgKey.K8S_SPARK_OPERATOR),
                extractConf(scheduledTaskDTO.getTaskProcessorArgs(), TaskProcessorArgKey.K8S_SPARK_OPERATOR));

    }

/*
    public static Map<String,String> extractK8sOperatorConfSpec(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO
    ){

        return extractConf(extractK8sOperatorConf(sourceConfDTO,scheduledTaskDTO) , "spec.");
    }
*/

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

            Map<String,Object> conf = extractK8sOperatorConf(sourceConfDTO,scheduledTaskDTO);
            Map<String,String> specSparkConf = SparkConfUtil.extractSparkConFromTaskConf(sourceConfDTO, scheduledTaskDTO);
            ConfigRenderOptions options = ConfigRenderOptions.defaults()
                .setJson(true)
                .setOriginComments(false)
                .setComments(false)
                .setFormatted(true);

            String a = ConfigFactory.parseMap(conf).root().render(options);

            K8sSparkApplicationConf k8SSparkApplicationConf = null;
            k8SSparkApplicationConf = ObjectMapping.stringToObject(a, K8sSparkApplicationConf.class);

            //todo next time need create getTaskFullName function in jijava  "k8s.spark-operator.metadata.name" : {{ k8s_dns_rfc_1123(scheduledTask)}}
            if (k8SSparkApplicationConf.getMetadata().getName()==null||k8SSparkApplicationConf.getMetadata().getName().isBlank())
                k8SSparkApplicationConf.getMetadata()
                        .setName(
                                String.format(
                                        "task-%d-%d",
                                        scheduledTaskDTO.getId(),
                                        scheduledTaskDTO.getTryNum()));

            k8SSparkApplicationConf.getSpec().setSparkConf(specSparkConf);


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
            resultList.add(ObjectMapping.asJsonString(scheduledTaskDTO));
        } catch (JsonProcessingException e) {
            throw new TaskConfigurationException(e);
        }
        resultList.addAll(
            Coalesce.applyMergeNonNullValuesMap(
                sourceConfDTO.getTargetDataSource().getService().getProperties(),
                scheduledTaskDTO.getTaskProcessorArgs())
                .entrySet()
                .stream()
                .filter(e ->
                        !e.getKey().startsWith(TaskProcessorArgKey.K8S_SPARK_OPERATOR) &&
                        !e.getKey().startsWith(TaskProcessorArgKey.SPARK_PREFIX)
                )
                .map(e-> String.format("--%s=%s",e.getKey(),e.getValue()))
                .toList());
        return resultList.toArray(String[]::new);
    }
}
