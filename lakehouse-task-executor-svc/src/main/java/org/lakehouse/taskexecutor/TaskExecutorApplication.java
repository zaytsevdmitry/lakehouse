package org.lakehouse.taskexecutor;

import org.lakehouse.client.rest.config.configuration.ConfigRestClientConfiguration;
import org.lakehouse.client.rest.scheduler.configuration.SchedulerRestClientConfiguration;
import org.lakehouse.client.rest.state.configuration.StateRestClientConfiguration;
import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.taskexecutor.configuration.ScheduledTaskKafkaConfigurationProperties;
import org.lakehouse.taskexecutor.configuration.SparkConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(value = {
        ScheduledTaskKafkaConfigurationProperties.class,
        SparkConfigurationProperties.class})
@ComponentScan(
        basePackages = {
                "org.lakehouse.taskexecutor",
                "org.lakehouse.client.rest.state"
        },
        basePackageClasses = {
                ConfigRestClientConfiguration.class,
                SchedulerRestClientConfiguration.class,
                StateRestClientConfiguration.class,
                JinJavaConfiguration.class})
public class TaskExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskExecutorApplication.class, args);
    }
}
