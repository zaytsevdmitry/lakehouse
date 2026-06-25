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

package org.lakehouse.validator.config;

import org.lakehouse.client.api.dto.configs.schedule.ScenarioActTemplateDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScenarioActTemplateConfValidator {
//todo move to common and apply in client

    public static ValidationResult validate(ScenarioActTemplateDTO scenarioActTemplateDTO) {

        List<String> descriptions = new ArrayList<>();

        descriptions.addAll(ScheduleConfValidator.validateEdges(
                String.format("ScenarioActTemplate %s", scenarioActTemplateDTO.getKeyName()),
                scenarioActTemplateDTO
                        .getTasks()
                        .stream()
                        .map(TaskDTO::getName).collect(Collectors.toSet()), scenarioActTemplateDTO.getDagEdges()
        ));

        ValidationResult result = new ValidationResult(descriptions.isEmpty(), descriptions);

        ScheduleConfValidator
                .validateEdges(
                        String.format("scenario template '%s'", scenarioActTemplateDTO.getKeyName()),
                        scenarioActTemplateDTO.getTasks().stream().map(TaskDTO::getName).collect(Collectors.toSet()),
                        scenarioActTemplateDTO.getDagEdges())
                .forEach(s -> {
                    result.setValid(false);
                    result.getDescriptions().add(s);
                });

        return result;
    }
}
