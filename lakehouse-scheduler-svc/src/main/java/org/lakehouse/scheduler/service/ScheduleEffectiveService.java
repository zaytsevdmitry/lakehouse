package org.lakehouse.scheduler.service;

import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
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

    public ScheduleEffectiveDTO getScheduleEffectiveDTO(String name){
        if ( !scheduleEffectiveDTOMap.containsKey(name))
            scheduleEffectiveDTOMap.put(name, configRestClientApi.getScheduleEffectiveDTO(name));

        return scheduleEffectiveDTOMap.get(name);
    }

    public ScheduleEffectiveDTO setScheduleEffectiveDTO(ScheduleEffectiveDTO scheduleEffectiveDTO){
        if ( !scheduleEffectiveDTOMap.containsKey(scheduleEffectiveDTO.getName())
        || scheduleEffectiveDTOMap
                .get(scheduleEffectiveDTO.getName()).getLastChangeNumber()
                < scheduleEffectiveDTO.getLastChangeNumber()
        )
            scheduleEffectiveDTOMap.put(scheduleEffectiveDTO.getName(),scheduleEffectiveDTO);
        return scheduleEffectiveDTOMap.get(scheduleEffectiveDTO.getName());
    }

    public boolean isBefore(String intervalExpression, OffsetDateTime lastOffsetDateTime){
        try {
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime next =DateTimeUtils.getNextTargetExecutionDateTime(intervalExpression, lastOffsetDateTime);
            logger.info("interval is {}\nlastOffsetDateTime={}\n              next={}\n               now={}\n",intervalExpression,lastOffsetDateTime,next,now);
            return next.isBefore(OffsetDateTime.now());
        } catch (CronParceErrorException e) {
            logger.error("Error when parsing cron statement in intervalExpression {}",intervalExpression,e);
            return false;
        }
    }
}
