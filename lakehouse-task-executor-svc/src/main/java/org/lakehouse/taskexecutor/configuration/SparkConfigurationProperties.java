package org.lakehouse.taskexecutor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lakehouse.client.rest.spark.server")
public class SparkConfigurationProperties extends AbstractConfigurationProperties {
}
