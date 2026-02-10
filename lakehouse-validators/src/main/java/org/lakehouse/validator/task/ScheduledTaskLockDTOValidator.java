package org.lakehouse.validator.task;

import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.validator.config.ValidationResult;

public class ScheduledTaskLockDTOValidator {
    public static ValidationResult validate(ScheduledTaskLockDTO scheduledTaskLockDTO){
        ValidationResult result = new ValidationResult();
            if (scheduledTaskLockDTO == null)
                result.getDescriptions().add(String.format("%s can't be null", ScheduledTaskLockDTO.class.getName()));
            else {
                if (scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTargetDateTime() == null)
                    result.getDescriptions().add("scheduledTaskLockDTO.getScheduledTaskEffectiveDTO can't be null");
                if (scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getIntervalStartDateTime() == null)
                    result.getDescriptions().add("scheduledTaskLockDTO.getScheduledTaskEffectiveDTO.getIntervalStartDateTime can't be null");
                if (scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getIntervalEndDateTime() == null)
                    result.getDescriptions().add("scheduledTaskLockDTO.getScheduledTaskEffectiveDTO.getIntervalEndDateTime can't be null");
            }
            result.setValid(result.getDescriptions().isEmpty());
            return result;
    }
}
