package org.lakehouse.taskexecutor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "lakehouse.task-executor.scheduled.task.kafka.consumer")
public class ScheduledTaskKafkaConfigurationProperties {

    private  Map<String, String> properties = new HashMap<>();

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}

