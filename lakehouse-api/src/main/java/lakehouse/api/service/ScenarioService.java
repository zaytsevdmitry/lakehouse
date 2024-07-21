package lakehouse.api.service;

import jakarta.transaction.Transactional;
import lakehouse.api.dto.DagEdgeDTO;
import lakehouse.api.dto.ScenarioDTO;
import lakehouse.api.dto.TaskDTO;
import lakehouse.api.entities.DagTaskEdge;
import lakehouse.api.entities.ExecutionModuleArg;
import lakehouse.api.entities.ScenarioTemplate;
import lakehouse.api.entities.TaskTemplate;
import lakehouse.api.repository.DagTaskEdgeRepository;
import lakehouse.api.repository.ExecutionModuleArgRepository;
import lakehouse.api.repository.ScenarioTemplateRepository;
import lakehouse.api.repository.TaskExecutionServiceGroupRepository;
import lakehouse.api.repository.TaskTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScenarioService {
    private final ScenarioTemplateRepository scenarioTemplateRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final ExecutionModuleArgRepository executionModuleArgRepository;
    private final TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository;
    private final DagTaskEdgeRepository dagTaskEdgeRepository;

    public ScenarioService(
            ScenarioTemplateRepository scenarioTemplateRepository,
            TaskTemplateRepository taskTemplateRepository,
            ExecutionModuleArgRepository executionModuleArgRepository,
            TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository,
            DagTaskEdgeRepository dagTaskEdgeRepository) {
        this.scenarioTemplateRepository = scenarioTemplateRepository;
        this.taskTemplateRepository = taskTemplateRepository;
        this.executionModuleArgRepository = executionModuleArgRepository;
        this.taskExecutionServiceGroupRepository = taskExecutionServiceGroupRepository;
        this.dagTaskEdgeRepository = dagTaskEdgeRepository;
    }

    private ScenarioDTO mapScenarioToDTO(
            ScenarioTemplate scenarioTemplate
    ) {
        ScenarioDTO result = new ScenarioDTO();
        result.setName(scenarioTemplate.getName());
        result.setDescription(scenarioTemplate.getDescription());
        result.setTasks(
                taskTemplateRepository
                        .findByScenarioTemplateName(scenarioTemplate.getName())
                        .stream()
                        .map(taskTemplate -> {
                            TaskDTO taskDTO = new TaskDTO();
                            taskDTO.setName(taskTemplate.getName());
                            taskDTO.setDescription(taskTemplate.getDescription());
                            taskDTO.setImportance(taskTemplate.getImportance());
                            taskDTO.setExecutionModule(taskTemplate.getExecutionModule());
                            taskDTO.setTaskExecutionServiceGroupName(taskTemplate.getTaskExecutionServiceGroup().getName());

                            Map<String, String> executionModuleArgs = new HashMap<>();

                            executionModuleArgRepository
                                    .findByScenarioTemplateName(taskTemplate.getName())
                                    .forEach(taskTemplateProperty ->
                                            executionModuleArgs.put(
                                                    taskTemplateProperty.getKey(),
                                                    taskTemplateProperty.getValue()));

                            taskDTO.setExecutionModuleArgs(executionModuleArgs);
                            return taskDTO;
                        })
                        .toList());
        result.setDagEdges(
                dagTaskEdgeRepository
                        .findByScenarioTemplateName(scenarioTemplate.getName()).stream().map(dagTaskEdge -> {
                            DagEdgeDTO dagEdgeDTO = new DagEdgeDTO();
                            dagEdgeDTO.setFrom(dagTaskEdge.getFromTaskTemplate().getName());
                            dagEdgeDTO.setTo(dagTaskEdge.getToTaskTemplate().getName());
                            return dagEdgeDTO;
                        }).toList());
        return result;
    }

    private ScenarioTemplate mapScenarioToEntity(ScenarioDTO scenarioDTO) {
        ScenarioTemplate result = new ScenarioTemplate();
        result.setName(scenarioDTO.getName());
        result.setDescription(scenarioDTO.getDescription());

        return result;
    }

    public List<ScenarioDTO> findAll() {
        return scenarioTemplateRepository.findAll().stream().map(this::mapScenarioToDTO).toList();
    }

    @Transactional
    public ScenarioDTO save(ScenarioDTO scenarioDTO) {
        ScenarioTemplate scenarioTemplate = mapScenarioToEntity(scenarioDTO);
        ScenarioTemplate result = scenarioTemplateRepository.save(scenarioTemplate);

        Map<String, TaskTemplate> taskTemplates = new HashMap<>();

        scenarioDTO.getTasks().forEach(taskDTO -> {

            TaskTemplate taskTemplate = new TaskTemplate();
            taskTemplate.setScenarioTemplate(scenarioTemplate);
            taskTemplate.setName(taskDTO.getName());
            taskTemplate.setImportance(taskDTO.getImportance());
            taskTemplate.setExecutionModule(taskDTO.getExecutionModule());
            taskTemplate.setTaskExecutionServiceGroup(
                    taskExecutionServiceGroupRepository
                            .getReferenceById(taskDTO.getTaskExecutionServiceGroupName()));

            taskTemplate.setDescription(taskDTO.getDescription());
            taskTemplateRepository.save(taskTemplate);

            taskTemplates.put(taskTemplate.getName(), taskTemplate);

            taskDTO.getExecutionModuleArgs().forEach((k, v) -> {
                ExecutionModuleArg taskTemplateProperty = new ExecutionModuleArg();
                taskTemplateProperty.setTaskTemplate(taskTemplate);
                taskTemplateProperty.setKey(k);
                taskTemplateProperty.setValue(v);
                executionModuleArgRepository.save(taskTemplateProperty);
            });
        });

        scenarioDTO.getDagEdges().forEach(dagEdgeDTO -> {
            DagTaskEdge dagTaskEdge = new DagTaskEdge();
            dagTaskEdge.setScenarioTemplate(scenarioTemplate);
            dagTaskEdge.setFromTaskTemplate(taskTemplates.get(dagEdgeDTO.getFrom()));
            dagTaskEdge.setToTaskTemplate(taskTemplates.get(dagEdgeDTO.getTo()));
            dagTaskEdgeRepository.save(dagTaskEdge);
        });


        return mapScenarioToDTO(result);
    }

    public ScenarioDTO findById(String name) {
        return mapScenarioToDTO(
                scenarioTemplateRepository
                        .findById(name)
                        .orElseThrow());
    }

    @Transactional
    public void deleteById(String name) {
        scenarioTemplateRepository.deleteById(name);
    }

/*    private List<TaskTemplate> getTaskTemplateEntities(ScenarioDTO scenarioDTO) {
        ScenarioTemplate scenarioTemplate = mapScenarioToEntity(scenarioDTO);

        return scenarioDTO.getTasks().stream().map(taskDTO -> {

            return mapTaskTemplateEntity(taskDTO,scenarioTemplate);
        }).toList();
    }*/
/*    private TaskTemplate mapTaskTemplateEntity(
            TaskDTO taskDTO,
            ScenarioTemplate scenarioTemplate){

        TaskTemplate taskTemplate = new TaskTemplate();
        taskTemplate.setScenarioTemplate(scenarioTemplate);
        taskTemplate.setName(taskDTO.getName());
        taskTemplate.setImportance(taskDTO.getImportance());
        taskTemplate.setExecutionModule(taskDTO.getExecutionModule());
        taskTemplate.setTaskExecutionServiceGroup(
                taskExecutionServiceGroupRepository
                        .getReferenceById(taskDTO.getTaskExecutionServiceGroupname()));

        taskTemplate.setComment(taskDTO.getComment());
        return taskTemplate;
    }*/

    /*List<DagTaskEdge> getDagTaskEdges(
            ScenarioTemplate scenarioTemplate,
            List<DagEdgeDTO> dagEdgeDTOList,
            List<TaskTemplate> taskTemplates){

        Map<String,TaskTemplate> taskTemplateMap = new HashMap<>();
        taskTemplates.forEach(taskTemplate -> taskTemplateMap.put(taskTemplate.getName(),taskTemplate));
        return dagEdgeDTOList.stream().map(dagEdgeDTO -> {
            DagTaskEdge dagTaskEdge = new DagTaskEdge();
            dagTaskEdge.setFromTaskTemplate(taskTemplateMap.get(dagEdgeDTO.getFrom()));
            dagTaskEdge.setToTaskTemplate(taskTemplateMap.get(dagEdgeDTO.getTo()));
            dagTaskEdge.setScenarioTemplate(scenarioTemplate);
            return dagTaskEdge;
        }).toList();
    }*/


}
