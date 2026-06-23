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

package org.lakehouse.taskexecutor.spark.dq.service.producer;

import org.lakehouse.client.api.dto.dq.MetricDQStatusTestSetDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MetricDQTestSetProducerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final KafkaTemplate<Long, MetricDQStatusTestSetDTO> kafkaTemplate;
    private final String topic;

    public MetricDQTestSetProducerService(
            @Qualifier("metricDQTestSetDTOKafkaTemplate") KafkaTemplate<Long, MetricDQStatusTestSetDTO> kafkaTemplate,
            @Value("${lakehouse.taskexecutor.body.config.dq.kafka.producer.testSet.status.topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void send(MetricDQStatusTestSetDTO msg) {
        kafkaTemplate.send(topic, msg.id(), msg);
    }


}
