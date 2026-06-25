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
