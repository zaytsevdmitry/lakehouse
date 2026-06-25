/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        for (ScheduleProduceMessage scheduleProduceMessage: scheduleProduceMessages) {
            //scheduleProduceMessages.forEach(scheduleProduceMessage -> {
            ScheduleEffectiveDTO scheduleEffectiveDTO = scheduleService.findEffectiveScheduleDTOById(scheduleProduceMessage.getSchedule().getKeyName());
            scheduleConfigProducerService.send(scheduleEffectiveDTO);
            scheduleProduceMessageRepository.delete(scheduleProduceMessage);
            logger.info("Schedule config {} sent", scheduleProduceMessage.getSchedule().getKeyName());

            //});
        }
    }
}
