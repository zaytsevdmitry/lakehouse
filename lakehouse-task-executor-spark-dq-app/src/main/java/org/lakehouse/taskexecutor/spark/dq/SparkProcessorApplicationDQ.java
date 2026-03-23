package org.lakehouse.taskexecutor.spark.dq;

import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.config.configuration.ConfigRestClientConfiguration;
import org.lakehouse.task.executor.spark.api.body.ApplicationBodyStarter;
import org.lakehouse.task.executor.spark.api.configuration.SparkSessionConfiguration;
import org.lakehouse.task.executor.spark.api.service.CatalogActivatorService;
import org.lakehouse.taskexecutor.spark.dq.configuration.DqMetricConfigProducerKafkaConfigurationProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(
        basePackages = {
                "org.lakehouse.taskexecutor.spark.dq"
        },
        basePackageClasses = {
                ConfigRestClientConfiguration.class,
                SparkSessionConfiguration.class,
                CatalogActivatorService.class,
                DqMetricConfigProducerKafkaConfigurationProperties.class
        }
        )

@EnableConfigurationProperties(DqMetricConfigProducerKafkaConfigurationProperties.class)
public class SparkProcessorApplicationDQ {


    public static void main(String[] args) throws InterruptedException {
            new ApplicationBodyStarter().runAndStop(args,SparkProcessorApplicationDQ.class);
    }
}