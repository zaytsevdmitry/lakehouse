package lakehouse.api.service.configs;

import jakarta.transaction.Transactional;
import lakehouse.api.dto.configs.DagEdgeDTO;
import lakehouse.api.dto.configs.ScenarioActTemplateDTO;
import lakehouse.api.dto.configs.TaskDTO;
import lakehouse.api.entities.configs.ScenarioActTemplate;
import lakehouse.api.entities.configs.TaskTemplate;
import lakehouse.api.entities.configs.TaskTemplateEdge;
import lakehouse.api.entities.configs.TaskTemplateExecutionModuleArg;
import lakehouse.api.repository.configs.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScenarioService {
	private final ScenarioActTemplateRepository scenarioActTemplateRepository;
	private final TaskTemplateRepository taskTemplateRepository;
	private final ExecutionModuleArgRepository executionModuleArgRepository;
	private final TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository;
	private final TaskTemplateEdgeRepository taskTemplateEdgeRepository;

	public ScenarioService(ScenarioActTemplateRepository scenarioActTemplateRepository,
			TaskTemplateRepository taskTemplateRepository, ExecutionModuleArgRepository executionModuleArgRepository,
			TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository,
			TaskTemplateEdgeRepository taskTemplateEdgeRepository) {
		this.scenarioActTemplateRepository = scenarioActTemplateRepository;
		this.taskTemplateRepository = taskTemplateRepository;
		this.executionModuleArgRepository = executionModuleArgRepository;
		this.taskExecutionServiceGroupRepository = taskExecutionServiceGroupRepository;
		this.taskTemplateEdgeRepository = taskTemplateEdgeRepository;
	}

	private ScenarioActTemplateDTO mapScenarioToDTO(ScenarioActTemplate scenarioActTemplate) {
		ScenarioActTemplateDTO result = new ScenarioActTemplateDTO();
		result.setName(scenarioActTemplate.getName());
		result.setDescription(scenarioActTemplate.getDescription());
		result.setTasks(taskTemplateRepository.findByScenarioTemplateName(scenarioActTemplate.getName()).stream()
				.map(taskTemplate -> {
					TaskDTO taskDTO = new TaskDTO();
					taskDTO.setName(taskTemplate.getName());
					taskDTO.setDescription(taskTemplate.getDescription());
					taskDTO.setImportance(taskTemplate.getImportance());
					taskDTO.setExecutionModule(taskTemplate.getExecutionModule());
					taskDTO.setTaskExecutionServiceGroupName(taskTemplate.getTaskExecutionServiceGroup().getName());

					Map<String, String> executionModuleArgs = new HashMap<>();

					executionModuleArgRepository.findByScenarioTemplateName(taskTemplate.getName())
							.forEach(taskTemplateProperty -> executionModuleArgs.put(taskTemplateProperty.getKey(),
									taskTemplateProperty.getValue()));

					taskDTO.setExecutionModuleArgs(executionModuleArgs);
					return taskDTO;
				}).toList());
		result.setDagEdges(taskTemplateEdgeRepository.findByScenarioTemplateName(scenarioActTemplate.getName()).stream()
				.map(taskTemplateEdge -> {
					DagEdgeDTO dagEdgeDTO = new DagEdgeDTO();
					dagEdgeDTO.setFrom(taskTemplateEdge.getFromTaskTemplate().getName());
					dagEdgeDTO.setTo(taskTemplateEdge.getToTaskTemplate().getName());
					return dagEdgeDTO;
				}).toList());
		return result;
	}

	private ScenarioActTemplate mapScenarioToEntity(ScenarioActTemplateDTO scenarioActTemplateDTO) {
		ScenarioActTemplate result = new ScenarioActTemplate();
		result.setName(scenarioActTemplateDTO.getName());
		result.setDescription(scenarioActTemplateDTO.getDescription());

		return result;
	}

	public List<ScenarioActTemplateDTO> findAll() {
		return scenarioActTemplateRepository.findAll().stream().map(this::mapScenarioToDTO).toList();
	}

	@Transactional
	public ScenarioActTemplateDTO save(ScenarioActTemplateDTO scenarioActTemplateDTO) {
		ScenarioActTemplate scenarioActTemplate = mapScenarioToEntity(scenarioActTemplateDTO);
		ScenarioActTemplate result = scenarioActTemplateRepository.save(scenarioActTemplate);

		Map<String, TaskTemplate> taskTemplates = new HashMap<>();

		scenarioActTemplateDTO.getTasks().forEach(taskDTO -> {

			TaskTemplate taskTemplate = new TaskTemplate();
			taskTemplate.setScenarioTemplate(scenarioActTemplate);
			taskTemplate.setName(taskDTO.getName());
			taskTemplate.setImportance(taskDTO.getImportance());
			taskTemplate.setExecutionModule(taskDTO.getExecutionModule());
			taskTemplate.setTaskExecutionServiceGroup(
					taskExecutionServiceGroupRepository.getReferenceById(taskDTO.getTaskExecutionServiceGroupName()));

			taskTemplate.setDescription(taskDTO.getDescription());
			taskTemplateRepository.save(taskTemplate);

			taskTemplates.put(taskTemplate.getName(), taskTemplate);

			taskDTO.getExecutionModuleArgs().forEach((k, v) -> {
				TaskTemplateExecutionModuleArg executionModuleArg = new TaskTemplateExecutionModuleArg();
				executionModuleArg.setTaskTemplate(taskTemplate);
				executionModuleArg.setKey(k);
				executionModuleArg.setValue(v);
				executionModuleArgRepository.save(executionModuleArg);
			});
		});

		scenarioActTemplateDTO.getDagEdges().forEach(dagEdgeDTO -> {
			TaskTemplateEdge taskTemplateEdge = new TaskTemplateEdge();
			taskTemplateEdge.setScenarioActTemplate(scenarioActTemplate);
			taskTemplateEdge.setFromTaskTemplate(taskTemplates.get(dagEdgeDTO.getFrom()));
			taskTemplateEdge.setToTaskTemplate(taskTemplates.get(dagEdgeDTO.getTo()));
			taskTemplateEdgeRepository.save(taskTemplateEdge);
		});

		return mapScenarioToDTO(result);
	}

	public ScenarioActTemplateDTO findById(String name) {
		return mapScenarioToDTO(scenarioActTemplateRepository.findById(name).orElseThrow());
	}

	@Transactional
	public void deleteById(String name) {
		scenarioActTemplateRepository.deleteById(name);
	}
}
