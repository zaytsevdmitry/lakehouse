package org.lakehouse.taskexecutor;
import org.lakehouse.client.rest.config.ConfigRestClientApiImpl;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.lakehouse.client.rest.scheduler.configuration.SchedulerRestClientConfiguration;
import org.lakehouse.client.rest.config.configuration.ConfigRestClientConfiguration;
@SpringBootApplication
@EnableScheduling
@ComponentScan(
        basePackages = "org.lakehouse.taskexecutor",
        basePackageClasses = {
        ConfigRestClientConfiguration.class,
        SchedulerRestClientConfiguration.class })
public class TaskExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskExecutorApplication.class, args);
    }
}
