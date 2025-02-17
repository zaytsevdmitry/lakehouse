package org.lakehouse.scheduler.configuration;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.api.serialization.schedule.ScheduleEffectiveKafkaSerializer;
import org.lakehouse.client.api.serialization.task.ScheduledTaskMsgKafkaSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ScheduledTaskDTOKafkaProducerConfiguration {
    @Value("${lakehouse.scheduler.schedule.task.kafka.producer.properties.bootstrap.servers}" )
    private String bootstrapServers;
    @Bean
    public ProducerFactory<Long, ScheduledTaskMsgDTO> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ScheduledTaskMsgKafkaSerializer.class);
        // See https://kafka.apache.org/documentation/#producerconfigs for more properties
        return props;
    }
    @Bean
    public KafkaTemplate<Long, ScheduledTaskMsgDTO> ScheduleEffectiveDTOKafkaTemplate() {
        return new KafkaTemplate<Long, ScheduledTaskMsgDTO>( producerFactory());
    }





}
