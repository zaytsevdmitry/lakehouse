package org.lakehouse.scheduler.service;

import org.lakehouse.client.api.dto.tasks.ScheduledTaskMsgDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTaskDTOProducerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final KafkaTemplate<Long, ScheduledTaskMsgDTO> kafkaTemplate;
    private final String scheduledTaskTopic;

    public ScheduledTaskDTOProducerService(
            KafkaTemplate<Long, ScheduledTaskMsgDTO> kafkaTemplate,
            @Value("${lakehouse.scheduler.schedule.task.kafka.producer.topic}") String scheduledTaskTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.scheduledTaskTopic = scheduledTaskTopic;
    }

    public void send (ScheduledTaskMsgDTO msg){
        kafkaTemplate.send(scheduledTaskTopic,msg.getId(),msg);
    }


}
