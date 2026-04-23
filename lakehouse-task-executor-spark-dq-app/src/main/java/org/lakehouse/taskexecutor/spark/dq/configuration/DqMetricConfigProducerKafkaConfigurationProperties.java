package org.lakehouse.taskexecutor.spark.dq.configuration;

import org.lakehouse.common.ConfigurationPropertiesAbstract;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;


@ConfigurationProperties(prefix = "lakehouse.taskexecutor.body.config.dq.kafka.producer")
public class DqMetricConfigProducerKafkaConfigurationProperties extends ConfigurationPropertiesAbstract {
}

