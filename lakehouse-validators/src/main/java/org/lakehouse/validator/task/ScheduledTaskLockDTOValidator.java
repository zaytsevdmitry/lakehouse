package org.lakehouse.validator.task;

import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.validator.config.ValidationResult;

public class ScheduledTaskLockDTOValidator {
    public static ValidationResult validate(ScheduledTaskLockDTO scheduledTaskLockDTO){
        ValidationResult result = new ValidationResult();
            if (scheduledTaskLockDTO == null)
                result.getDescriptions().add(String.format("%s can't be null", ScheduledTaskLockDTO.class.getName()));
            else {
                result.getDescriptions()
                        .addAll(ScheduledTaskDTOValidator
                                .validate(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO()).getDescriptions());
            }
            result.setValid(result.getDescriptions().isEmpty());
            return result;
    }
}
