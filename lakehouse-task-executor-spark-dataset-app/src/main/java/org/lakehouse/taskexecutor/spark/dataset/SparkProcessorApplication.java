package org.lakehouse.taskexecutor.spark.dataset;

import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.config.configuration.ConfigRestClientConfiguration;
import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.task.executor.spark.api.body.ApplicationBodyStarter;
import org.lakehouse.task.executor.spark.api.configuration.SparkSessionConfiguration;
import org.lakehouse.task.executor.spark.api.service.CatalogActivatorService;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(
        basePackages = {
                "org.lakehouse.taskexecutor.api",
                "org.lakehouse.taskexecutor.spark"
        },
        basePackageClasses = {
                ConfigRestClientConfiguration.class,
                    JinJavaConfiguration.class,
                SparkSessionConfiguration.class,
                CatalogActivatorService.class
        })
public class SparkProcessorApplication {

    public static void main(String[] args) throws InterruptedException {

        new ApplicationBodyStarter().runAndStop(args, SparkProcessorApplication.class);

    }
}