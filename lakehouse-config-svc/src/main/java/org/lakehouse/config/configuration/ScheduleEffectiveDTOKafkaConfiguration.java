package org.lakehouse.config.configuration;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.serialization.ScheduleEffectiveKafkaSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ScheduleEffectiveDTOKafkaConfiguration {
    @Value("${lakehouse.config.schedule.kafka.producer.bootstrap-servers}" )
    private String bootstrapServers;
    @Bean
    public ProducerFactory<String, ScheduleEffectiveDTO> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ScheduleEffectiveKafkaSerializer.class);
        // See https://kafka.apache.org/documentation/#producerconfigs for more properties
        return props;
    }
    @Bean
    public KafkaTemplate<String, ScheduleEffectiveDTO> ScheduleEffectiveDTOKafkaTemplate() {
        Producer<String, ScheduleEffectiveDTO> p = producerFactory().createProducer();

        return new KafkaTemplate<String, ScheduleEffectiveDTO>(producerFactory());
    }





}
