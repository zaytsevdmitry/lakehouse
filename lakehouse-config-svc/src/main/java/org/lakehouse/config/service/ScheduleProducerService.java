package org.lakehouse.config.service;

import org.lakehouse.cli.api.dto.configs.ScheduleEffectiveDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScheduleProducerService {

    private final KafkaTemplate<String, ScheduleEffectiveDTO> scheduleEffectiveDTOKafkaTemplate;
    private final String scheduleTopic;

    public ScheduleProducerService(
            KafkaTemplate<String, ScheduleEffectiveDTO> scheduleEffectiveDTOKafkaTemplate,
            @Value("${lakehouse.config.schedule.kafka.producer.topic}") String scheduleTopic) {
        this.scheduleEffectiveDTOKafkaTemplate = scheduleEffectiveDTOKafkaTemplate;
        this.scheduleTopic = scheduleTopic;
    }

    public void send (ScheduleEffectiveDTO msg){
        scheduleEffectiveDTOKafkaTemplate.send(scheduleTopic,msg.getName(),msg);
    }
}
