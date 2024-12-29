package org.lakehouse.scheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.kafka.core.KafkaTemplate;
@Service
public class ScheduleProducer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final KafkaTemplate kafkaTemplate;
    public ScheduleProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public void sendMessage(String message) {
        logger.info(String.format("#### -> Producing message -> %s", message));
        //todo fix me
        this.kafkaTemplate.send("fixme", message);
    }
}
