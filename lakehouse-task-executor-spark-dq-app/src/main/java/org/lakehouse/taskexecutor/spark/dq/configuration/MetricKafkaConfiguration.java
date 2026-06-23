/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
