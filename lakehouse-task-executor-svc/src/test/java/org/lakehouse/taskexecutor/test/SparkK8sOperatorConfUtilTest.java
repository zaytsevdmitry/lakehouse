package org.lakehouse.taskexecutor.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.processor.spark.k8s.operator.SparkK8sOperatorConfUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class SparkK8sOperatorConfUtilTest {
    
    
    private SourceConfDTO getSourceConfDTO() {
        DriverDTO driverDTO = new DriverDTO();
        driverDTO.setKeyName("spark_iceberg");
        driverDTO.setConnectionTemplates(Map.of(
            Types.ConnectionType.spark, "{%set service=dataSources[dataSets[targetDataSetKeyName].dataSourceKeyName].service%}{{taskProcessorArgs.protocol}}://{{service.host}}:{{service.port}}"
                ));

        ServiceDTO serviceDTO = new ServiceDTO();

        Map<String,String> props = new HashMap<>( Map.of(
                "spark.driver.extraClassPath", "/opt/drivers/postgresql-42.7.8.jar,/opt/drivers/iceberg-spark-runtime-3.5_2.12-1.9.2.jar,/opt/drivers/hadoop-aws-3.3.4.jar,/opt/drivers/aws-java-sdk-bundle-1.12.262.jar,/opt/drivers/wildfly-openssl-1.0.7.Final.jar",
                "spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem",
                "spark.eventLog.enabled", "true",
                "spark.eventLog.dir", "s3a://sparklogs/eventlog/",
                "k8s.spark-operator.manifest.spec.driver.cores", "1",
                "k8s.spark-operator.manifest.metadata.namespace", "lakehouse-management"
        ));

        serviceDTO.setProperties(props);
        serviceDTO.setHost("test-host-name");
        serviceDTO.setPort("8443");

        DataSourceDTO targetSourceDTO = new DataSourceDTO();
        targetSourceDTO.setKeyName("targetSource");
        targetSourceDTO.setService(serviceDTO);
        targetSourceDTO.setDriverKeyName(driverDTO.getKeyName());

        DataSetDTO targetDataSetDTO = new DataSetDTO();
        targetDataSetDTO.setKeyName("targetDataSet");
        targetDataSetDTO.setDataSourceKeyName(targetSourceDTO.getKeyName());


        SourceConfDTO sourceConfDTO = new SourceConfDTO();
        sourceConfDTO.setTargetDataSetKeyName(targetDataSetDTO.getKeyName());
        sourceConfDTO.setDataSources(Map.of(targetSourceDTO.getKeyName(),targetSourceDTO));
        sourceConfDTO.setDataSets(Map.of(targetDataSetDTO.getKeyName(), targetDataSetDTO));
        sourceConfDTO.setDrivers(Map.of(driverDTO.getKeyName(),driverDTO));
        return sourceConfDTO;

    }
    
    private ScheduledTaskDTO getScheduledTaskDTO(){
        ScheduledTaskDTO scheduledTaskDTO = new ScheduledTaskDTO();
        scheduledTaskDTO.setName("quality");
        scheduledTaskDTO.setScheduleKeyName("Test_Schedule");
        scheduledTaskDTO.setScenarioActKeyName("Test_Act");
        scheduledTaskDTO.setTargetDateTime("2026-01-01 00:00:00z");
        scheduledTaskDTO.setTaskExecutionServiceGroupName("default");
        scheduledTaskDTO.setTaskProcessor("sparkK8sOperatorTaskProcessor");
        scheduledTaskDTO.setTaskProcessorBody( "sparkTaskProcessorDQBody");
        scheduledTaskDTO.setTryNum(1);
        scheduledTaskDTO.setId(100L);
        Map<String,String> taskProcessorArgs = Map.of(
                "k8s.spark-operator.manifest.spec.image", "apache/spark:3.5.0",
                "spark.ui.enabled", "true",
                "spark.executor.memory", "1g",
                "k8s.spark-operator.manifest.metadata.namespace", "lakehouse-management-ovrd",
                "lakehouse.client.rest.config.server.url", "http://lakehouse-management-config-service:8080",
                "lakehouse.taskexecutor.body.config.dq.kafka.producer.properties.bootstrap.servers" , "broker:9092",
                "lakehouse.taskexecutor.body.config.dq.kafka.producer.metric.value.topic", "metric_value",
                "protocol", "https",
                "k8s.spark-operator.manifest.spec.mainApplicationFile", "/opt/lakehouse-task-spark-apps/lakehouse-task-executor-spark-dq-app-0.4.0-jar-with-dependencies.jar",
                "k8s.spark-operator.manifest.spec.mainClass", "org.lakehouse.taskexecutor.spark.dq.SparkProcessorApplicationDQ");


        scheduledTaskDTO.setTaskProcessorArgs(taskProcessorArgs);
        return scheduledTaskDTO;

    }
    
    
    
    @Test
    @Order(1)
    public void testK8sOperatorConfExtraction() {
        System.out.println("Map.of(\n");
        Map<String,String> result = SparkK8sOperatorConfUtil.extractK8sOperatorConf(getSourceConfDTO(),getScheduledTaskDTO());
        result.forEach((s, s2) -> System.out.println(String.format("\"%s\",\"%s\"",s,s2)));
        System.out.println(")\n");
        Map<String,String> expected = Map.of(
                "manifest.spec.driver.cores","1",
                "manifest.spec.mainApplicationFile","/opt/lakehouse-task-spark-apps/lakehouse-task-executor-spark-dq-app-0.4.0-jar-with-dependencies.jar",
                "manifest.spec.mainClass","org.lakehouse.taskexecutor.spark.dq.SparkProcessorApplicationDQ",
                "manifest.metadata.namespace","lakehouse-management-ovrd",
                "manifest.spec.image","apache/spark:3.5.0"
        )
                ;
        assert(Objects.equals(result,expected));
    }
    @Test
    @Order(2)
    public void testMasterUrlExtraction() throws JsonProcessingException, TaskConfigurationException {
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
        ScheduledTaskDTO scheduledTaskDTO = getScheduledTaskDTO();
        SourceConfDTO sourceConfDTO = getSourceConfDTO();
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(scheduledTaskDTO));
        String result = SparkK8sOperatorConfUtil.extractMasterUrl(sourceConfDTO,scheduledTaskDTO,jinJavaUtils);
        String expected = "https://test-host-name:8443";
        System.out.println(String.format("result masterUrl:%s",result));
        System.out.println(String.format("expected masterUrl:%s",expected));
        assert (expected.equals(result));
    }    @Test
    @Order(2)
    public void testAppJsonExtraction() throws IOException, TaskConfigurationException {
        Map<String,String>  result = ObjectMapping.stringToObject(
                SparkK8sOperatorConfUtil.extractAppConfJson(getSourceConfDTO(),getScheduledTaskDTO()),
                HashMap.class);
        Map<String,String> expected = ObjectMapping.stringToObject("""
  {
  "apiVersion" : null,
  "kind" : null,
  "metadata" : {
    "name" : "task-100-1",
    "namespace" : "lakehouse-management-ovrd"
  },
  "spec" : {
    "driver" : {
      "cores" : 1,
      "memory" : null,
      "serviceAccount" : null
    },
    "executor" : null,
    "image" : "apache/spark:3.5.0",
    "mainApplicationFile" : "/opt/lakehouse-task-spark-apps/lakehouse-task-executor-spark-dq-app-0.4.0-jar-with-dependencies.jar",
    "mainClass" : "org.lakehouse.taskexecutor.spark.dq.SparkProcessorApplicationDQ",
    "mode" : null,
    "sparkConf" : {
      "spark.driver.extraClassPath" : "/opt/drivers/postgresql-42.7.8.jar,/opt/drivers/iceberg-spark-runtime-3.5_2.12-1.9.2.jar,/opt/drivers/hadoop-aws-3.3.4.jar,/opt/drivers/aws-java-sdk-bundle-1.12.262.jar,/opt/drivers/wildfly-openssl-1.0.7.Final.jar",
      "spark.eventLog.dir" : "s3a://sparklogs/eventlog/",
      "spark.eventLog.enabled" : "true",
      "spark.executor.memory" : "1g",
      "spark.hadoop.fs.s3a.impl" : "org.apache.hadoop.fs.s3a.S3AFileSystem",
      "spark.ui.enabled" : "true"
    },
    "arguments" : [ "{\\n  \\"name\\" : \\"quality\\",\\n  \\"taskExecutionServiceGroupName\\" : \\"default\\",\\n  \\"taskProcessor\\" : \\"sparkK8sOperatorTaskProcessor\\",\\n  \\"taskProcessorBody\\" : \\"sparkTaskProcessorDQBody\\",\\n  \\"importance\\" : null,\\n  \\"description\\" : null,\\n  \\"taskProcessorArgs\\" : { },\\n  \\"id\\" : 100,\\n  \\"scenarioActKeyName\\" : \\"Test_Act\\",\\n  \\"scheduleKeyName\\" : \\"Test_Schedule\\",\\n  \\"status\\" : null,\\n  \\"targetDateTime\\" : \\"2026-01-01 00:00:00z\\",\\n  \\"intervalStartDateTime\\" : null,\\n  \\"intervalEndDateTime\\" : null,\\n  \\"dataSetKeyName\\" : null,\\n  \\"tryNum\\" : 1\\n}", "--lakehouse.client.rest.config.server.url=http://lakehouse-management-config-service:8080", "--lakehouse.taskexecutor.body.config.dq.kafka.producer.metric.value.topic=metric_value", "--protocol=https", "--lakehouse.taskexecutor.body.config.dq.kafka.producer.properties.bootstrap.servers=broker:9092" ],
    "sparkVersion" : null,
    "type" : null
  }
}""", HashMap.class);
        System.out.println(String.format("result: %s",ObjectMapping.asJsonString(result)));
        System.out.println(String.format("expected :%s",ObjectMapping.asJsonString(expected)));
        assert (expected.equals(result));
    }
}
