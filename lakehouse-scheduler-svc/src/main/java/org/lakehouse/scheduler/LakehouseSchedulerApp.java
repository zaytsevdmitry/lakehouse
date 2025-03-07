package org.lakehouse.scheduler;

import org.lakehouse.scheduler.configuration.ScheduleConfigConsumerKafkaConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages= {
				"org.lakehouse.scheduler",
				"org.lakehouse.client.rest.config"})

@EnableConfigurationProperties(value = ScheduleConfigConsumerKafkaConfigurationProperties.class)
public class LakehouseSchedulerApp {
    public static void main(String[] args) {
        SpringApplication.run(LakehouseSchedulerApp.class, args);
    }
}