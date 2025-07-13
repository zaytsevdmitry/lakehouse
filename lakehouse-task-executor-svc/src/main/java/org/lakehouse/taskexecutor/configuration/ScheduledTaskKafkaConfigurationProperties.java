package org.lakehouse.taskexecutor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lakehouse.task-executor.scheduled.task.kafka.consumer")
public class ScheduledTaskKafkaConfigurationProperties extends AbstractConfigurationProperties {

}

