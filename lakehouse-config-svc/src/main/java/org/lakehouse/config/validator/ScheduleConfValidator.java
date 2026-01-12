package org.lakehouse.config.validator;

import org.lakehouse.client.api.dto.configs.DagEdgeDTO;
import org.lakehouse.client.api.dto.configs.ScheduleDTO;
import org.lakehouse.client.api.dto.configs.ScheduleScenarioActAbstract;
import org.lakehouse.client.api.dto.configs.TaskDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ScheduleConfValidator {
//todo move to common and apply in client

    public static List<String> validateEdges(
            String objectDescription,
            Set<String> vertices,
            Set<DagEdgeDTO> edges) {
        List<String> descriptions = new ArrayList<>();
        edges.forEach(dagEdgeDTO -> {
            if (!vertices.contains(dagEdgeDTO.getFrom()))
                descriptions.add(String.format("Error in %s. Scenario 'From' of %s not found", objectDescription, dagEdgeDTO));
            if (!vertices.contains(dagEdgeDTO.getTo()))
                descriptions.add(String.format("Error in %s. Scenario  'To' of %s not found", objectDescription, dagEdgeDTO));
        });

        return descriptions;
    }

    public static ValidationResult validate(ScheduleDTO scheduleDTO) {
        List<String> descriptions = new ArrayList<>();
        Set<String> acts = scheduleDTO
                .getScenarioActs()
                .stream()
                .map(ScheduleScenarioActAbstract::getName)
                .collect(Collectors.toSet());

        descriptions
                .addAll(
                        validateEdges(
                                String.format("Schedule %s", scheduleDTO.getName()),
                                scheduleDTO
                                        .getScenarioActs()
                                        .stream()
                                        .map(ScheduleScenarioActAbstract::getName)
                                        .collect(Collectors.toSet()),
                                scheduleDTO.getScenarioActEdges()));

        if (scheduleDTO.getScenarioActs().isEmpty())
            descriptions.add("Error Scenario is empty");

        scheduleDTO.getScenarioActs().forEach(ssa -> {
            descriptions
                    .addAll(
                            validateEdges(
                                    String.format("Scenario Act  %s.%s", scheduleDTO.getName(), ssa.getName()),
                                    ssa
                                            .getTasks()
                                            .stream()
                                            .map(TaskDTO::getName)
                                            .collect(Collectors.toSet()),
                                    ssa.getDagEdges()));
            if (ssa.getDataSet() == null || ssa.getDataSet().isEmpty())
                descriptions.add(String.format("Error %S.%S. DataSet key name is empty", scheduleDTO.getName(), ssa.getName()));

            if (ssa.getIntervalStart() == null || ssa.getIntervalStart().isEmpty())
                descriptions.add(String.format("Error %S.%S. intervalStart key name is empty", scheduleDTO.getName(), ssa.getName()));
            if (ssa.getIntervalEnd() == null || ssa.getIntervalEnd().isEmpty())
                descriptions.add(String.format("Error %S.%S. intervalEnd key name is empty", scheduleDTO.getName(), ssa.getName()));


        });
        return new ValidationResult(descriptions.isEmpty(), descriptions);
    }
}
