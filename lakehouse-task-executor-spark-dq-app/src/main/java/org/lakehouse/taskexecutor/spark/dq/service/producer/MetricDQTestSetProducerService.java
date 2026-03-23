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
