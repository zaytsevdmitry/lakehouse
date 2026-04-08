package org.lakehouse.config.configuration;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.lakehouse.client.api.serialization.schedule.ScheduleEffectiveKafkaSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ScheduleChangeMsgDTOProducerConfiguration {

    @Bean
    public KafkaTemplate<String, ScheduleEffectiveDTO> scheduleEffectiveDTOKafkaTemplate(
            ScheduleConfKafkaProducerConfigurationProperties scheduleConfKafkaProducerConfigurationProperties) {
        Map<String, Object> props = new HashMap<>(scheduleConfKafkaProducerConfigurationProperties.getProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ScheduleEffectiveKafkaSerializer.class);
        ProducerFactory<String, ScheduleEffectiveDTO> producerFactory = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<String, ScheduleEffectiveDTO>(producerFactory);
    }


}
