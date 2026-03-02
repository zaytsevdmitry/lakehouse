package org.lakehouse.taskexecutor.spark.dq.configuration;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.lakehouse.client.api.dto.dq.MetricDQStatusDTO;
import org.lakehouse.client.api.dto.dq.MetricDQStatusTestSetDTO;
import org.lakehouse.client.api.dto.dq.MetricDQValueDTO;
import org.lakehouse.client.api.serialization.dq.metric.MetricDQKafkaSerializer;
import org.lakehouse.client.api.serialization.dq.metric.MetricDQTestSetKafkaSerializer;
import org.lakehouse.client.api.serialization.dq.metric.MetricDQValueKafkaSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MetricKafkaConfiguration {

    @Autowired
    DqMetricConfigProducerKafkaConfigurationProperties dqMetricConfigProducerKafkaConfigurationProperties;

    @Bean(value = "metricDQDTOKafkaTemplate")
    public KafkaTemplate<Long, MetricDQStatusDTO> metricDQDTOKafkaTemplate() {
        Map<String, Object> producerConfig = new HashMap<>(dqMetricConfigProducerKafkaConfigurationProperties.getProperties());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MetricDQKafkaSerializer.class);
        return new KafkaTemplate<Long, MetricDQStatusDTO>( new DefaultKafkaProducerFactory<>(producerConfig));
    }

    @Bean(value = "metricDQTestSetDTOKafkaTemplate")
    public KafkaTemplate<Long, MetricDQStatusTestSetDTO> metricDQTestSetDTOKafkaTemplate() {
        Map<String, Object> producerConfig = new HashMap<>(dqMetricConfigProducerKafkaConfigurationProperties.getProperties());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MetricDQTestSetKafkaSerializer.class);
        return new KafkaTemplate<Long, MetricDQStatusTestSetDTO>( new DefaultKafkaProducerFactory<>(producerConfig));
    }

    @Bean(value = "metricDQValueDTOKafkaTemplate")
    public KafkaTemplate<Long, MetricDQValueDTO> metricDQValueDTOKafkaTemplate() {
        Map<String, Object> producerConfig = new HashMap<>(dqMetricConfigProducerKafkaConfigurationProperties.getProperties());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MetricDQValueKafkaSerializer.class);
        return new KafkaTemplate<Long, MetricDQValueDTO>( new DefaultKafkaProducerFactory<>(producerConfig));
    }
}
