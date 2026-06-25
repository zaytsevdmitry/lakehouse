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
import org.lakehouse.client.api.exception.CronParceErrorException;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScheduleEffectiveService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Map<String, ScheduleEffectiveDTO> scheduleEffectiveDTOMap = new ConcurrentHashMap<>();
    private final ConfigRestClientApi configRestClientApi;

    public ScheduleEffectiveService(ConfigRestClientApi configRestClientApi) {
        this.configRestClientApi = configRestClientApi;
    }

    public ScheduleEffectiveDTO getScheduleEffectiveDTO(String name) {
        if (!scheduleEffectiveDTOMap.containsKey(name))
            scheduleEffectiveDTOMap.put(name, configRestClientApi.getScheduleEffectiveDTO(name));

        return scheduleEffectiveDTOMap.get(name);

    }

    public ScheduleEffectiveDTO setScheduleEffectiveDTO(ScheduleEffectiveDTO scheduleEffectiveDTO) {
        if (!scheduleEffectiveDTOMap.containsKey(scheduleEffectiveDTO.getKeyName())
                || scheduleEffectiveDTOMap
                .get(scheduleEffectiveDTO.getKeyName()).getLastChangeNumber()
                < scheduleEffectiveDTO.getLastChangeNumber()
        )
            scheduleEffectiveDTOMap.put(scheduleEffectiveDTO.getKeyName(), scheduleEffectiveDTO);
        return scheduleEffectiveDTOMap.get(scheduleEffectiveDTO.getKeyName());
    }

    public boolean isBefore(String intervalExpression, OffsetDateTime lastOffsetDateTime) {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime next = DateTimeUtils.getNextTargetExecutionDateTime(intervalExpression, lastOffsetDateTime);
            logger.info("interval is {}\nlastOffsetDateTime={}\n              next={}\n               now={}\n", intervalExpression, lastOffsetDateTime, next, now);
            return next.isBefore(OffsetDateTime.now());
        } catch (CronParceErrorException e) {
            logger.error("Error when parsing cron statement in intervalExpression {}", intervalExpression, e);
            return false;
        }
    }
}
