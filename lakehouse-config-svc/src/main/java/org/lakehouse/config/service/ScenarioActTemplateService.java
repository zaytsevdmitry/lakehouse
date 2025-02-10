package org.lakehouse.config.service;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.DagEdgeDTO;
import org.lakehouse.client.api.dto.configs.ScenarioActTemplateDTO;

import org.lakehouse.client.api.dto.configs.TaskDTO;
import org.lakehouse.config.entities.TaskAbstract;
import org.lakehouse.config.entities.templates.ScenarioActTemplate;
import org.lakehouse.config.entities.templates.TaskTemplate;
import org.lakehouse.config.entities.templates.TaskTemplateEdge;
import org.lakehouse.config.entities.templates.TaskTemplateExecutionModuleArg;
import org.lakehouse.config.exception.TaskTemplateNotFoundException;
import org.lakehouse.config.mapper.Mapper;
import org.lakehouse.config.repository.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ScenarioActTemplateService {
	private final ScenarioActTemplateRepository scenarioActTemplateRepository;
	private final TaskTemplateRepository taskTemplateRepository;
	private final TaskTemplateExecutionModuleArgRepository executionModuleArgRepository;
	private final TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository;
	private final TaskTemplateEdgeRepository taskTemplateEdgeRepository;
	private final Mapper mapper;

	public ScenarioActTemplateService(ScenarioActTemplateRepository scenarioActTemplateRepository,
									  TaskTemplateRepository taskTemplateRepository,
									  TaskTemplateExecutionModuleArgRepository executionModuleArgRepository,
									  TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository,
									  TaskTemplateEdgeRepository taskTemplateEdgeRepository,
									  Mapper mapper) {
		this.scenarioActTemplateRepository = scenarioActTemplateRepository;
		this.taskTemplateRepository = taskTemplateRepository;
		this.executionModuleArgRepository = executionModuleArgRepository;
		this.taskExecutionServiceGroupRepository = taskExecutionServiceGroupRepository;
		this.taskTemplateEdgeRepository = taskTemplateEdgeRepository;
		this.mapper = mapper;
	}

	public TaskDTO findTaskByScenarioActTemplateAndTaskName(
			String scenarioActTemplateName,
			String taskName
	){

		Optional<TaskTemplate> taskTemplate =  taskTemplateRepository.findByScenarioActTemplateNameAndName(scenarioActTemplateName,taskName);
		if (taskTemplate.isPresent()) {

			TaskTemplate t = taskTemplate.orElseThrow();
			Map<String,String> args = getTaskTemplateExecutionModuleArgsByTaskTemplateId(t.getId());
			return mapper.mapTaskToDTO(t, args );
		}
		return null;
	}

	private ScenarioActTemplateDTO mapScenarioToDTO(ScenarioActTemplate scenarioActTemplate) {
		ScenarioActTemplateDTO result = new ScenarioActTemplateDTO();
		result.setName(scenarioActTemplate.getName());
		result.setDescription(scenarioActTemplate.getDescription());
		result.setTasks(taskTemplateRepository.findByScenarioTemplateName(scenarioActTemplate.getName()).stream()
				.map(taskTemplate -> mapper
							 .mapTaskToDTO(
									 taskTemplate,
									 getTaskTemplateExecutionModuleArgsByTaskTemplateId(taskTemplate.getId()))
				).toList());

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

	public Map<String,ScenarioActTemplateDTO> findAllAsMap() {
		return findAll().stream().collect(Collectors.toMap(ScenarioActTemplateDTO::getName,scenarioActTemplateDTO -> scenarioActTemplateDTO));
	}

	@Transactional
	public ScenarioActTemplateDTO save(ScenarioActTemplateDTO scenarioActTemplateDTO) {
		ScenarioActTemplate scenarioActTemplate = mapScenarioToEntity(scenarioActTemplateDTO);
		taskTemplateRepository.findByScenarioTemplateName(scenarioActTemplate.getName()).forEach(taskTemplateRepository::delete);
		taskTemplateEdgeRepository.findByScenarioTemplateName(scenarioActTemplate.getName()).forEach(taskTemplateEdgeRepository::delete);
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

	public Optional<TaskTemplate> findTaskTemplateByScenarioAndName(String scenarioActTemplateName, String taskTemplateName){
		return taskTemplateRepository.findByScenarioActTemplateNameAndName(scenarioActTemplateName,taskTemplateName);
		//		.orElseThrow(() -> new TaskTemplateNotFoundException(scenarioActTemplateName,taskTemplateName));
	}

	/*
	public List<TaskTemplateExecutionModuleArg> getTaskTemplateExecutionModuleArgsByTaskTemplateId(Long id){
		return executionModuleArgRepository.findByTaskTemplateId(id);
	}
	*/

	public Map<String,String> getTaskTemplateExecutionModuleArgsByTaskTemplateId(Long taskTemplateId){
		return executionModuleArgRepository.findByTaskTemplateId(taskTemplateId)
				.stream()
				.collect(
						Collectors
								.toMap(
										TaskTemplateExecutionModuleArg::getKey,
										TaskTemplateExecutionModuleArg::getValue));
	}


			/*public List<TaskTemplateExecutionModuleArg> getTaskTemplateExecutionModuleArgsByTemplateAndTask(
			String scenarioActTemplateName,
			String taskTemplateName){
		return executionModuleArgRepository
				.findByScenarioTemplateNameAndTaskTemplateName(scenarioActTemplateName,taskTemplateName);
	*/
}
