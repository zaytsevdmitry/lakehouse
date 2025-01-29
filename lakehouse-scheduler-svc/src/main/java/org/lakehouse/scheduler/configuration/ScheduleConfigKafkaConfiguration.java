package org.lakehouse.scheduler.configuration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.serialization.ScheduleEffectiveKafkaDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ScheduleConfigKafkaConfiguration {
    @Value("${lakehouse.scheduler.config.schedule.kafka.consumer.bootstrap-servers}" )
    private String bootstrapServers;
    @Value("${lakehouse.scheduler.config.schedule.kafka.consumer.group-id}" )
    private String consumerGroup;
    @Value("${lakehouse.scheduler.config.schedule.kafka.consumer.auto-offset-reset}" )
    private String autoOffsetReset;

    private Map<String, Object> consumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ScheduleEffectiveKafkaDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
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
