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

import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.validator.config.ValidationResult;
import org.springframework.util.StringUtils;
public class TaskDTOValidator {

    public static ValidationResult validate(TaskDTO taskDTO)  {
        ValidationResult result = new ValidationResult();
        if (!StringUtils.hasText(taskDTO.getTemplate())){
            if (!StringUtils.hasText(taskDTO.getTaskExecutionServiceGroupName()))
                result.getDescriptions().add(String.format("Task %s.The \"taskExecutionServiceGroupName\" field is required", taskDTO.getName()));
            if (!StringUtils.hasText(taskDTO.getTaskProcessor()))
                result.getDescriptions().add(String.format("Task %s.The \"taskProcessor\" field is required", taskDTO.getName()));
            if (!StringUtils.hasText(taskDTO.getName()))
                result.getDescriptions().add("The \"name\" field is required");
        }
        return result;
    }
}
