package org.lakehouse.config.component;


import org.lakehouse.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.lakehouse.config.entities.ScheduleProduceMessage;
import org.lakehouse.config.repository.ScheduleProduceMessageRepository;
import org.lakehouse.config.service.ScheduleConfigProducerService;
import org.lakehouse.config.service.ScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EnableScheduling
@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class InternalScheduler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScheduleConfigProducerService scheduleConfigProducerService;
    private final ScheduleProduceMessageRepository scheduleProduceMessageRepository;
    private final ScheduleService scheduleService;
    private final Integer sendLimit;

    public InternalScheduler(
            ScheduleConfigProducerService scheduleConfigProducerService,
            ScheduleProduceMessageRepository scheduleProduceMessageRepository, ScheduleService scheduleService,
            @Value("${lakehouse.config.schedule.send.limit}")
            Integer sendLimit
    ) {

        this.scheduleConfigProducerService = scheduleConfigProducerService;
        this.scheduleProduceMessageRepository = scheduleProduceMessageRepository;
        this.scheduleService = scheduleService;
        logger.info("sendLimit {}", sendLimit);
        this.sendLimit = sendLimit;
    }

    /**
     * Made schedule objects with status  NEW
     */
    @Scheduled(
            fixedDelayString = "${lakehouse.config.schedule.send.delay-ms}",
            initialDelayString = "${lakehouse.config.schedule.send.initial-delay-ms}")
    public void build() {

        List<ScheduleProduceMessage> scheduleProduceMessages = scheduleProduceMessageRepository.findAllWithLimit(Limit.of(sendLimit));
        logger.info("Found {} schedule config for send", scheduleProduceMessages.size());
        scheduleProduceMessages.forEach(scheduleProduceMessage -> {
            ScheduleEffectiveDTO scheduleEffectiveDTO = scheduleService.findEffectiveScheduleDTOById(scheduleProduceMessage.getSchedule().getKeyName());
            scheduleConfigProducerService.send(scheduleEffectiveDTO);
            scheduleProduceMessageRepository.delete(scheduleProduceMessage);
        });
    }
}
