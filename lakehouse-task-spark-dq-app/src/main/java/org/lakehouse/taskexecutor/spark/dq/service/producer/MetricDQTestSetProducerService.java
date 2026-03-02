package org.lakehouse.taskexecutor.spark.dq.service.producer;

import org.lakehouse.client.api.dto.dq.MetricDQStatusDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MetricDQTestSetProducerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final KafkaTemplate<Long, MetricDQStatusDTO> kafkaTemplate;
    private final String topic;

    public MetricDQTestSetProducerService(
            @Qualifier("metricDQTestSetDTOKafkaTemplate") KafkaTemplate<Long, MetricDQStatusDTO> kafkaTemplate,
            @Value("${lakehouse.taskexecutor.body.config.dq.kafka.producer.testSet.status.topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void send(MetricDQStatusDTO msg) {
        kafkaTemplate.send(topic, msg.Id(), msg);
    }


}
