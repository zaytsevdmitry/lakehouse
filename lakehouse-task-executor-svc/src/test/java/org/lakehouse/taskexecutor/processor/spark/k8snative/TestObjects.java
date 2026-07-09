package org.lakehouse.taskexecutor.processor.spark.k8snative;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;

import java.util.HashMap;
import java.util.Map;

public class TestObjects {

    public static SourceConfDTO getSourceConfDTO() {
        DriverDTO driverDTO = new DriverDTO();
        driverDTO.setKeyName("spark_iceberg");
        driverDTO.setConnectionTemplates(Map.of(
                Types.ConnectionType.spark, "{%set service=dataSources[dataSets[targetDataSetKeyName].dataSourceKeyName].service%}{%set protocol=taskProcessorArgs['datasource.service.protocol']%}{{protocol}}://{{service.host}}:{{service.port}}"
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
    public static ScheduledTaskDTO getScheduledTaskDTO(){
        ScheduledTaskDTO scheduledTaskDTO = new ScheduledTaskDTO();
        scheduledTaskDTO.setName("quality");
        scheduledTaskDTO.setScheduleKeyName("Test_Schedule");
        scheduledTaskDTO.setScenarioActKeyName("Test_Act");
        scheduledTaskDTO.setTargetDateTime("2026-01-01 00:00:00z");
        scheduledTaskDTO.setTaskExecutionServiceGroupName("default");
        scheduledTaskDTO.setTaskProcessor("K8sSparkNativeTaskProcessor");
        scheduledTaskDTO.setTaskProcessorBody( "sparkTaskProcessorDQBody");
        Map<String,String> taskProcessorArgs = new HashMap<>(
                Map.of(
                        "k8s.spark-operator.manifest.spec.image", "apache/spark:3.5.0",
                        "spark.ui.enabled", "true",
                        "spark.executor.memory", "1g",
                        "k8s.spark-operator.manifest.metadata.namespace", "lakehouse-management-ovrd",
                        "lakehouse.client.rest.config.server.url", "http://lakehouse-management-config-service:8080",
                        "lakehouse.taskexecutor.body.config.dq.kafka.producer.properties.bootstrap.servers" , "broker:9092",
                        "lakehouse.taskexecutor.body.config.dq.kafka.producer.metric.value.topic", "metric_value",
                        "datasource.service.protocol", "https",
                        "k8s.spark-operator.manifest.spec.mainApplicationFile", "/opt/lakehouse-task-spark-apps/lakehouse-task-executor-spark-dq-app-0.5.0-jar-with-dependencies.jar",
                        "k8s.spark-operator.manifest.spec.mainClass", "org.lakehouse.taskexecutor.spark.dq.SparkProcessorApplicationDQ"));

        scheduledTaskDTO.setId(123L);
        scheduledTaskDTO.setTryNum(2);

        scheduledTaskDTO.setTaskProcessorArgs(taskProcessorArgs);
        return scheduledTaskDTO;

    }
}
