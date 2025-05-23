package org.lakehouse.scheduler.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;


@ConfigurationProperties(prefix = "lakehouse.scheduler.config.schedule.kafka.consumer")
public class ScheduleConfigConsumerKafkaConfigurationProperties {

    private Map<String, String> properties;

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

}

