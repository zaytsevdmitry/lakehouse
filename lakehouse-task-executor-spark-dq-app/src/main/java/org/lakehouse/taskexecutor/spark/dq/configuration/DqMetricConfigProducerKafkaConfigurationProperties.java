package org.lakehouse.taskexecutor.spark.dq.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;


@ConfigurationProperties(prefix = "lakehouse.taskexecutor.body.config.dq.kafka.producer")
public class DqMetricConfigProducerKafkaConfigurationProperties {

    private Map<String, String> properties;

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

}

