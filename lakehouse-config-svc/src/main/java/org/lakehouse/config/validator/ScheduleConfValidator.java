package org.lakehouse.config.validator;

import org.lakehouse.client.api.dto.configs.DagEdgeDTO;
import org.lakehouse.client.api.dto.configs.ScheduleDTO;
import org.lakehouse.client.api.dto.configs.ScheduleScenarioActAbstract;
import org.lakehouse.client.api.dto.configs.TaskDTO;

import java.util.*;
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
    public static Map<String,List<String>> EdgesToMap(Set<DagEdgeDTO> dagEdgeDTOs) {
        Map<String,List<String>> result = new HashMap<>();

        dagEdgeDTOs.forEach(dagEdgeDTO -> {
            if(result.containsKey(dagEdgeDTO.getFrom())){
                List<String> l = new ArrayList<>();
                l.addAll(result.get(dagEdgeDTO.getFrom()));
                l.add(dagEdgeDTO.getTo());
                result.put(dagEdgeDTO.getFrom(), l);
            }else {
                result.put(dagEdgeDTO.getFrom(),Arrays.asList(dagEdgeDTO.getTo()));
            }
        });
        return result;
    }
    public static List<String> validateEdges(Map<String,List<String>> edges){
        List<String> result = new ArrayList<>();
        Set<String> vertices =
                edges.entrySet()
                        .stream()
                        .map(e ->{
                            List<String> l = new ArrayList<>();
                            l.addAll(e.getValue());
                            l.add(e.getKey());
                            return l;})
                        .flatMap(Collection::stream).collect(Collectors.toSet());

        for (String vertice:vertices){
            if (isCycle(vertice,vertice,edges)) {
                result.add(String.format("Vertices %s аre cycled",vertice));
            }
        }
        return result;
    }
    public static boolean isCycle(String verticeTarget,String verticeCurr, Map<String,List<String>> edges){
        if(edges.containsKey(verticeCurr)){
            for(String v:edges.get(verticeCurr)){
                System.out.println(verticeTarget +" >>--> key=" + verticeCurr + " >>---> value=" + v);
                if (v.equals(verticeTarget)){
                    return true;
                }else {
                    if ( isCycle(verticeTarget, v, edges)){
                        return true;
                    };
                }
            }
        }
        return false;
    }

}
