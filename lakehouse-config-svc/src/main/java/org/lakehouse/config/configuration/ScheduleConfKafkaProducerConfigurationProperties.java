package org.lakehouse.config.configuration;

import org.lakehouse.common.ConfigurationPropertiesAbstract;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 *
 *
 * */
@Component
@ConfigurationProperties(prefix = "lakehouse.config.schedule.send.kafka.producer")
public class ScheduleConfKafkaProducerConfigurationProperties extends ConfigurationPropertiesAbstract {

}

