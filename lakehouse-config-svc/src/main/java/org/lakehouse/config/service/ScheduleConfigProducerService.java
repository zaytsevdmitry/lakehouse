package org.lakehouse.config.service;

import org.lakehouse.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.lakehouse.config.entities.Schedule;
import org.lakehouse.config.entities.ScheduleProduceMessage;
import org.lakehouse.config.repository.ScheduleProduceMessageRepository;
import org.lakehouse.config.repository.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScheduleConfigProducerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final KafkaTemplate<String, ScheduleEffectiveDTO> scheduleEffectiveDTOKafkaTemplate;
    private final String scheduleTopic;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleProduceMessageRepository scheduleProduceMessageRepository;

    public ScheduleConfigProducerService(
            KafkaTemplate<String, ScheduleEffectiveDTO> scheduleEffectiveDTOKafkaTemplate,
            //todo rename namespace. kafka.producer. must contain only kafka producer native properties!!!!
            @Value("${lakehouse.config.schedule.kafka.producer.schedule.send.topic}") String scheduleTopic,
            @Value("${lakehouse.config.schedule.kafka.producer.schedule.delete.topic}") String scheduleDeleteTopic,
            ScheduleRepository scheduleRepository, ScheduleProduceMessageRepository scheduleProduceMessageRepository
    ) {
        this.scheduleEffectiveDTOKafkaTemplate = scheduleEffectiveDTOKafkaTemplate;
        this.scheduleTopic = scheduleTopic;
        this.scheduleRepository = scheduleRepository;
        this.scheduleProduceMessageRepository = scheduleProduceMessageRepository;

    }

    public void send(ScheduleEffectiveDTO msg) {
        scheduleEffectiveDTOKafkaTemplate.send(scheduleTopic, msg.getName(), msg);
    }

    public void changeSchedule(Schedule schedule) {
        ScheduleProduceMessage scheduleProduceMessage = new ScheduleProduceMessage();
        scheduleProduceMessage.setSchedule(schedule);
        scheduleProduceMessageRepository.save(scheduleProduceMessage);
    }


}
