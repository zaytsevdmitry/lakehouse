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
