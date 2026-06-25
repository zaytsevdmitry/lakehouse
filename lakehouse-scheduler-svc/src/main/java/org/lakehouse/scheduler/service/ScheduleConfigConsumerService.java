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

package org.lakehouse.scheduler.service;

import org.lakehouse.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
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
        logger.info("New schedule config change: {}", scheduleEffectiveDTO.getKeyName());
        buildService.registration(scheduleEffectiveDTO);
        logger.info("findAndRegisterNewSchedules");
    }
}
