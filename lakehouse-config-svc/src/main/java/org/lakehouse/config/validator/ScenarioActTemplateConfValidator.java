package org.lakehouse.config.validator;

import org.lakehouse.client.api.dto.configs.ScenarioActTemplateDTO;
import org.lakehouse.client.api.dto.configs.TaskDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScenarioActTemplateConfValidator {
//todo move to common and apply in client

    public static ValidationResult validate(ScenarioActTemplateDTO scenarioActTemplateDTO) {
        List<String> descriptions = new ArrayList<>();
        descriptions.addAll(ScheduleConfValidator.validateEdges(
                String.format("ScenarioActTemplate %s", scenarioActTemplateDTO.getName()),
                scenarioActTemplateDTO
                        .getTasks()
                        .stream()
                        .map(TaskDTO::getName).collect(Collectors.toSet()), scenarioActTemplateDTO.getDagEdges()

        ));
        return new ValidationResult(descriptions.isEmpty(), descriptions);
    }
}
