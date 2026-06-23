/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
