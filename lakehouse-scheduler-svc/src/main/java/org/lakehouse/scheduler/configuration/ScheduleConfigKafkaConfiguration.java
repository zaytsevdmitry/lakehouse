package org.lakehouse.scheduler.configuration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.serialization.schedule.ScheduleEffectiveKafkaDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ScheduleConfigKafkaConfiguration {

    @Autowired
    ScheduleConfigConsumerKafkaConfigurationProperties scheduleConfigConsumerKafkaConfigurationProperties;

     private Map<String, Object> consumerProps() {
         Map<String, Object> props = new HashMap<>(scheduleConfigConsumerKafkaConfigurationProperties
                 .getProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ScheduleEffectiveKafkaDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return props;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ScheduleEffectiveDTO> containerFactory() {
        ConcurrentKafkaListenerContainerFactory<String,  ScheduleEffectiveDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<String, ScheduleEffectiveDTO>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumerProps()));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.getContainerProperties().setSyncCommits(true);
        return factory;
    }
}
