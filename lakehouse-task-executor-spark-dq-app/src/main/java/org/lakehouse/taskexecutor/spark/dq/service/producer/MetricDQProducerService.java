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

package org.lakehouse.taskexecutor.spark.dq.service.producer;

import org.lakehouse.client.api.dto.dq.MetricDQStatusDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MetricDQProducerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final KafkaTemplate<Long, MetricDQStatusDTO> kafkaTemplate;
    private final String topic;

    private String ofNull(Object o){
        if (o == null){
            return "<NULL>";
        }
        else return o.toString();
    }
    public MetricDQProducerService(
            @Qualifier("metricDQDTOKafkaTemplate") KafkaTemplate<Long, MetricDQStatusDTO> kafkaTemplate,
            @Value("${lakehouse.taskexecutor.body.config.dq.kafka.producer.metric.status.topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        kafkaTemplate.getProducerFactory().getConfigurationProperties().forEach((s, o) -> System.out.println(s + "--#>" +ofNull(o)));
    }

    public void send(MetricDQStatusDTO msg) {
        kafkaTemplate.getProducerFactory().getConfigurationProperties().forEach((s, o) -> System.out.println(s + "--*>" +ofNull(o)));
        kafkaTemplate.send(topic, msg.metricId(), msg);
    }
}
