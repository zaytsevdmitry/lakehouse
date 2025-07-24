package org.lakehouse.config.service;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.DagEdgeDTO;
import org.lakehouse.client.api.dto.configs.ScenarioActTemplateDTO;
import org.lakehouse.client.api.dto.configs.TaskDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.config.entities.scenario.ScenarioAct;
import org.lakehouse.config.entities.templates.ScenarioActTemplate;
import org.lakehouse.config.entities.templates.TaskTemplate;
import org.lakehouse.config.entities.templates.TaskTemplateEdge;
import org.lakehouse.config.entities.templates.TaskTemplateExecutionModuleArg;
import org.lakehouse.config.mapper.Mapper;
import org.lakehouse.config.repository.*;
import org.lakehouse.config.validator.ConfDTOValidationException;
import org.lakehouse.config.validator.ScenarioActTemplateConfValidator;
import org.lakehouse.config.validator.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScenarioActTemplateService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final ScenarioActTemplateRepository scenarioActTemplateRepository;
	private final TaskTemplateRepository taskTemplateRepository;
	private final TaskTemplateExecutionModuleArgRepository executionModuleArgRepository;
	private final TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository;
	private final TaskTemplateEdgeRepository taskTemplateEdgeRepository;
	private final ScenarioActRepository scenarioActRepository;
	private final ScheduleRepository scheduleRepository;
	private final ScheduleConfigProducerService scheduleConfigProducerService;
	private final Mapper mapper;

	public ScenarioActTemplateService(ScenarioActTemplateRepository scenarioActTemplateRepository,
                                      TaskTemplateRepository taskTemplateRepository,
                                      TaskTemplateExecutionModuleArgRepository executionModuleArgRepository,
                                      TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository,
                                      TaskTemplateEdgeRepository taskTemplateEdgeRepository, ScenarioActRepository scenarioActRepository, ScheduleRepository scheduleRepository, org.lakehouse.config.service.ScheduleConfigProducerService scheduleConfigProducerService,
                                      Mapper mapper) {
		this.scenarioActTemplateRepository = scenarioActTemplateRepository;
		this.taskTemplateRepository = taskTemplateRepository;
		this.executionModuleArgRepository = executionModuleArgRepository;
		this.taskExecutionServiceGroupRepository = taskExecutionServiceGroupRepository;
		this.taskTemplateEdgeRepository = taskTemplateEdgeRepository;
        this.scenarioActRepository = scenarioActRepository;
        this.scheduleRepository = scheduleRepository;
        this.scheduleConfigProducerService = scheduleConfigProducerService;
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
		result.setName(scenarioActTemplate.getKeyName());
		result.setDescription(scenarioActTemplate.getDescription());
		result.setTasks(taskTemplateRepository.findByScenarioActTemplateKeyName(scenarioActTemplate.getKeyName()).stream()
				.map(taskTemplate -> mapper
							 .mapTaskToDTO(
									 taskTemplate,
									 getTaskTemplateExecutionModuleArgsByTaskTemplateId(taskTemplate.getId()))
				).toList());

		result.setDagEdges(taskTemplateEdgeRepository.findByScenarioActTemplateKeyName(scenarioActTemplate.getKeyName()).stream()
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
		result.setKeyName(scenarioActTemplateDTO.getName());
		result.setDescription(scenarioActTemplateDTO.getDescription());

		return result;
	}

	public List<ScenarioActTemplateDTO> findAll() {
		return scenarioActTemplateRepository.findAll().stream().map(this::mapScenarioToDTO).toList();
	}

	public Map<String,ScenarioActTemplateDTO> findAllAsMap() {
		return findAll().stream().collect(Collectors.toMap(ScenarioActTemplateDTO::getName,scenarioActTemplateDTO -> scenarioActTemplateDTO));
	}

	public List<TaskDTO> getTaskDTOListNullSafe(ScenarioActTemplateDTO scenarioActTemplateDTO){
		if ( scenarioActTemplateDTO != null)
			return scenarioActTemplateDTO.getTasks();
		else
			return new ArrayList<>();
	}


	public List<DagEdgeDTO> getDagEdgeDTOListNullSafe(ScenarioActTemplateDTO scenarioActTemplateDTO){
		if ( scenarioActTemplateDTO != null)
			return scenarioActTemplateDTO.getDagEdges();
		else
			return new ArrayList<>();
	}
	@Transactional
	public ScenarioActTemplateDTO save(ScenarioActTemplateDTO scenarioActTemplateDTO) {

		ValidationResult vr = ScenarioActTemplateConfValidator.validate(scenarioActTemplateDTO);

		if (!vr.isValid())
			throw new ConfDTOValidationException(vr.getDescriptions());

		ScenarioActTemplate scenarioActTemplate = mapScenarioToEntity(scenarioActTemplateDTO);
		taskTemplateEdgeRepository.findByScenarioActTemplateKeyName(scenarioActTemplate.getKeyName()).forEach(taskTemplateEdgeRepository::delete);
		taskTemplateRepository.findByScenarioActTemplateKeyName(scenarioActTemplate.getKeyName()).forEach(
				taskTemplate -> {
					logger.info("Delete task {}.{}",scenarioActTemplate.getKeyName(),taskTemplate.getName());
					taskTemplateRepository.delete(taskTemplate);
				});
		taskTemplateRepository.findByScenarioActTemplateKeyName(scenarioActTemplate.getKeyName()).forEach(taskTemplate ->
				logger.info("Found task {}.{}",scenarioActTemplate.getKeyName(),taskTemplate.getName()));
		logger.info("Save ScenarioActTemplate.name={}",scenarioActTemplateDTO.getName());
		ScenarioActTemplate result = scenarioActTemplateRepository.save(scenarioActTemplate);

		Map<String, TaskTemplate> taskTemplates = new HashMap<>();

		scenarioActTemplateDTO.getTasks().forEach(taskDTO -> {

			TaskTemplate taskTemplateBefore = new TaskTemplate();
			taskTemplateBefore.setScenarioTemplate(scenarioActTemplate);
			taskTemplateBefore.setName(taskDTO.getName());
			taskTemplateBefore.setImportance(taskDTO.getImportance());
			taskTemplateBefore.setExecutionModule(taskDTO.getExecutionModule());
			taskTemplateBefore.setTaskExecutionServiceGroup(
					taskExecutionServiceGroupRepository.getReferenceById(taskDTO.getTaskExecutionServiceGroupName()));

			taskTemplateBefore.setDescription(taskDTO.getDescription());

			logger.info("Save ScenarioActTemplate.name={}, taskTemplate.name={}",scenarioActTemplateDTO.getName(),taskTemplateBefore.getName());

			TaskTemplate taskTemplateAfter = taskTemplateRepository.save(taskTemplateBefore);

			taskTemplates.put(taskTemplateBefore.getName(), taskTemplateBefore);

			taskDTO.getExecutionModuleArgs().forEach((k, v) -> {

				logger.info("Save ScenarioActTemplate.name={}, taskTemplate.name={}, argKey={}" ,
						scenarioActTemplateDTO.getName(),
						taskTemplateAfter.getName(),
						k);
				TaskTemplateExecutionModuleArg executionModuleArg = new TaskTemplateExecutionModuleArg();
				executionModuleArg.setTaskTemplate(taskTemplateAfter);
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

		scenarioActRepository
				.findByScenarioActTemplateKeyName(scenarioActTemplate.getKeyName())
				.stream()
				.map(ScenarioAct::getSchedule)
				.collect(Collectors.toSet())
				.forEach(schedule -> {
							schedule.setLastChangeNumber(schedule.getLastChangeNumber() + 1);
							schedule.setLastChangedDateTime(DateTimeUtils.now());
							scheduleConfigProducerService.changeSchedule(scheduleRepository.save(schedule));
						}

					);

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
	}


	public Map<String,String> getTaskTemplateExecutionModuleArgsByTaskTemplateId(Long taskTemplateId){
		return executionModuleArgRepository.findByTaskTemplateId(taskTemplateId)
				.stream()
				.collect(
						Collectors
								.toMap(
										TaskTemplateExecutionModuleArg::getKey,
										TaskTemplateExecutionModuleArg::getValue));
	}

}
