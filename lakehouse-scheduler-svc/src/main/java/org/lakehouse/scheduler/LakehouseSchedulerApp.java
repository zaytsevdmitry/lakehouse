package org.lakehouse.scheduler;

import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.scheduler.configuration.ScheduleConfigConsumerKafkaConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {
        "org.lakehouse.scheduler",
        "org.lakehouse.client.rest.config"},
scanBasePackageClasses = {JinJavaConfiguration.class})

@EnableConfigurationProperties(value = {ScheduleConfigConsumerKafkaConfigurationProperties.class})
public class LakehouseSchedulerApp {
    public static void main(String[] args) {
        SpringApplication.run(LakehouseSchedulerApp.class, args);
    }
}