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
