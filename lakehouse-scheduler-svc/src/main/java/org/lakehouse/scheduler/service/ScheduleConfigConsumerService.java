package org.lakehouse.scheduler.service;

import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ScheduleConfigConsumerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final BuildService buildService;

    private final ConfigRestClientApi configRestClientApi;

    public ScheduleConfigConsumerService(
            BuildService buildService, ConfigRestClientApi configRestClientApi) {
        this.buildService = buildService;
        this.configRestClientApi = configRestClientApi;
    }


    @KafkaListener(
            topics = "#{'${lakehouse.scheduler.config.schedule.kafka.consumer.topics}'.split(',')}",
            concurrency = "#{'${lakehouse.scheduler.config.schedule.kafka.consumer.concurrency}'}",
            containerFactory = "containerFactory")
    public void listen(ScheduleEffectiveDTO scheduleEffectiveDTO) {
        logger.info("New schedule config change: {}", scheduleEffectiveDTO.getName());
        buildService.registration(scheduleEffectiveDTO);
        logger.info("findAndRegisterNewSchedules");
    }
}
