/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.taskexecutor.spark.dq.configuration;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.lakehouse.client.api.dto.dq.MetricDQStatusDTO;
import org.lakehouse.client.api.dto.dq.MetricDQStatusTestSetDTO;
import org.lakehouse.client.api.dto.dq.MetricDQValueDTO;
import org.lakehouse.client.api.serialization.dq.metric.MetricDQStatusKafkaSerializer;
import org.lakehouse.client.api.serialization.dq.metric.MetricDQTestSetKafkaSerializer;
import org.lakehouse.client.api.serialization.dq.metric.MetricDQValueKafkaSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MetricKafkaConfiguration {

    private final DqMetricConfigProducerKafkaConfigurationProperties dqMetricConfigProducerKafkaConfigurationProperties;

    public MetricKafkaConfiguration(DqMetricConfigProducerKafkaConfigurationProperties dqMetricConfigProducerKafkaConfigurationProperties) {
        this.dqMetricConfigProducerKafkaConfigurationProperties = dqMetricConfigProducerKafkaConfigurationProperties;
    }

    @Bean(value = "metricDQDTOKafkaTemplate")
    public KafkaTemplate<Long, MetricDQStatusDTO> metricDQDTOKafkaTemplate() {
        Map<String, Object> producerConfig = new HashMap<>(dqMetricConfigProducerKafkaConfigurationProperties.getProperties());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MetricDQStatusKafkaSerializer.class);
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
