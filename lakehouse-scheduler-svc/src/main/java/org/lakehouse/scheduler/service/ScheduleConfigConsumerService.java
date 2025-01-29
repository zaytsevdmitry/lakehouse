package org.lakehouse.scheduler.service;

import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ScheduleConfigConsumerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ScheduleInstanceLastBuildService scheduleInstanceLastBuildService;

    public ScheduleConfigConsumerService(
            ScheduleInstanceLastBuildService scheduleInstanceLastBuildService) {
        this.scheduleInstanceLastBuildService = scheduleInstanceLastBuildService;
    }

    @KafkaListener(
            topics = "#{'${lakehouse.scheduler.config.schedule.kafka.consumer.topics}'.split(',')}",
            concurrency = "#{'${lakehouse.scheduler.config.schedule.kafka.consumer.concurrency}'}",
            containerFactory = "containerFactory"
    )
    public void listen(ScheduleEffectiveDTO scheduleEffectiveDTO)
    {
        logger.info("New schedule config change: " + scheduleEffectiveDTO.getName());
        scheduleInstanceLastBuildService.findAndRegisterNewSchedule(scheduleEffectiveDTO);
        logger.info("findAndRegisterNewSchedules");
    }

}
