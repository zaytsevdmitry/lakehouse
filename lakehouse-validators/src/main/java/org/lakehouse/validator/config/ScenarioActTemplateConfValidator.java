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
