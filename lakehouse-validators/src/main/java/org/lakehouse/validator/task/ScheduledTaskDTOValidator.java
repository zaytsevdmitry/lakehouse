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
