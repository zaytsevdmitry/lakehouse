package org.lakehouse.validator.task;

import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.validator.config.ValidationResult;

public class ScheduledTaskDTOValidator {
    public static ValidationResult validate(ScheduledTaskDTO scheduledTaskDTO){
        ValidationResult result = new ValidationResult();
            if (scheduledTaskDTO == null)
                result.getDescriptions().add(String.format("%s can't be null", ScheduledTaskDTO.class.getName()));
            else {
                if (scheduledTaskDTO.getTargetDateTime() == null)
                    result.getDescriptions().add(String.format("%s.getScheduledTaskEffectiveDTO can't be null", ScheduledTaskDTO.class.getSimpleName()));
                if (scheduledTaskDTO.getIntervalStartDateTime() == null)
                    result.getDescriptions().add(String.format("%s.getScheduledTaskEffectiveDTO.getIntervalStartDateTime can't be null", ScheduledTaskDTO.class.getSimpleName()));
                if (scheduledTaskDTO.getIntervalEndDateTime() == null)
                    result.getDescriptions().add(String.format("%s.getScheduledTaskEffectiveDTO.getIntervalEndDateTime can't be null", ScheduledTaskDTO.class.getSimpleName()));
            }
            result.setValid(result.getDescriptions().isEmpty());
            return result;
    }
}
