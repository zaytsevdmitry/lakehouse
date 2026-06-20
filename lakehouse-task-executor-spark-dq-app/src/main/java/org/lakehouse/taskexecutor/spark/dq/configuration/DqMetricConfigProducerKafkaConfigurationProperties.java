package org.lakehouse.taskexecutor.spark.dq.configuration;

import org.lakehouse.common.ConfigurationPropertiesAbstract;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "lakehouse.taskexecutor.body.config.dq.kafka.producer")
public class DqMetricConfigProducerKafkaConfigurationProperties extends ConfigurationPropertiesAbstract {
}

