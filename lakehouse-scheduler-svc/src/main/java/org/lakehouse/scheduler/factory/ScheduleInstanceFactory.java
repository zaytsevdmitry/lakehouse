package org.lakehouse.scheduler.factory;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDTO;
import org.lakehouse.client.api.exception.CronParceErrorException;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.scheduler.entities.ScheduleInstance;
import org.lakehouse.scheduler.entities.ScheduleInstanceLastBuild;
import org.lakehouse.scheduler.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

public class ScheduleInstanceFactory {
    private final static Logger logger = LoggerFactory.getLogger(ScheduleInstanceFactory.class);
    public static ScheduleInstance newScheduleInstance(
            ScheduleInstanceLastBuild scheduleInstanceLast,
            ScheduleEffectiveDTO scheduleEffectiveDTO) {

        ScheduleInstance scheduleInstance = new ScheduleInstance();
        scheduleInstance.setConfigScheduleKeyName(scheduleInstanceLast.getConfigScheduleKeyName());
        OffsetDateTime lastTargetExecutionDate;

        if (scheduleInstanceLast.getScheduleInstance() == null) {
            lastTargetExecutionDate =  DateTimeUtils.parseDateTimeFormatWithTZ( scheduleEffectiveDTO.getStartDateTime()); //scheduleInstanceLast.getSchedule().getStartDateTime();
        } else {
            lastTargetExecutionDate = scheduleInstanceLast.getScheduleInstance().getTargetExecutionDateTime();
        }

        try {
            scheduleInstance.setTargetExecutionDateTime(DateTimeUtils.getNextTargetExecutionDateTime(
                    scheduleEffectiveDTO.getIntervalExpression(), lastTargetExecutionDate));
        } catch (CronParceErrorException e) {
            logger.warn(e.getMessage());
            throw new TransactionException(String.format("Error when try to set TargetExecutionDateTime of %s",
                    scheduleEffectiveDTO.getName()), e);
        }

        scheduleInstance.setStatus(Status.Schedule.NEW.label);

        return scheduleInstance;
    }

    public static ScheduleInstanceDTO mapScheduleInstanceDTO(ScheduleInstance scheduleInstance){
        ScheduleInstanceDTO result = new ScheduleInstanceDTO();
        result.setId(scheduleInstance.getId());
        result.setStatus(scheduleInstance.getStatus());
        result.setConfigScheduleKeyName(scheduleInstance.getConfigScheduleKeyName());
        result.setTargetExecutionDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(scheduleInstance.getTargetExecutionDateTime()));
        return result;
    }

    public static List<ScheduleInstanceDTO> scheduleInstanceDTOList(List<ScheduleInstance> scheduleInstanceList){
        return  scheduleInstanceList
                        .stream()
                        .map(ScheduleInstanceFactory::mapScheduleInstanceDTO)
                        .toList();
    }
}
