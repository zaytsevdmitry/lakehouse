package org.lakehouse.config.service;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.*;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.client.api.utils.DateTimeUtils;

import org.lakehouse.config.entities.scenario.ScenarioAct;
import org.lakehouse.config.entities.scenario.ScenarioActEdge;
import org.lakehouse.config.entities.scenario.ScenarioActTask;
import org.lakehouse.config.entities.scenario.ScenarioActTaskEdge;
import org.lakehouse.config.entities.scenario.ScenarioActTaskExecutionModuleArg;
import org.lakehouse.config.entities.Schedule;
import org.lakehouse.config.entities.templates.ScenarioActTemplate;
import org.lakehouse.config.exception.ScenarioActNotFoundException;
import org.lakehouse.config.exception.ScheduleNotFoundException;
import org.lakehouse.config.exception.TaskEffectiveNotFoundException;
import org.lakehouse.config.mapper.Mapper;
import org.lakehouse.config.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final ScheduleRepository scheduleRepository;
	private final DataSetRepository dataSetRepository;
	private final ScenarioActTemplateRepository scenarioActTemplateRepository;
	private final ScenarioActRepository scenarioActRepository;
	private final ScenarioActEdgeRepository scenarioActEdgeRepository;
	private final ScenarioActTaskRepository scenarioActTaskRepository;
	private final ScenarioActTaskEdgeRepository scenarioActTaskEdgeRepository;
	private final TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository;
	private final ScenarioActTemplateService scenarioActTemplateService;
	private final ScheduleConfigProducerService scheduleConfigProducerService;
	private final ScenarioActTaskExecutionModuleArgRepository scenarioActTaskExecutionModuleArgRepository;
	private final Mapper mapper;

	public ScheduleService(
            ScheduleRepository scheduleRepository,
            DataSetRepository dataSetRepository,
            ScenarioActTemplateRepository scenarioActTemplateRepository,
            ScenarioActRepository scenarioActRepository,
            ScenarioActEdgeRepository scenarioActEdgeRepository,
            ScenarioActTaskRepository scenarioActTaskRepository,
            ScenarioActTaskEdgeRepository scenarioActTaskEdgeRepository,
            TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository,
            ScenarioActTemplateService scenarioActTemplateService,
			ScheduleConfigProducerService scheduleConfigProducerService,
			ScenarioActTaskExecutionModuleArgRepository scenarioActTaskExecutionModuleArgRepository,
            Mapper mapper) {
		this.scheduleRepository = scheduleRepository;
		this.dataSetRepository = dataSetRepository;
		this.scenarioActTemplateRepository = scenarioActTemplateRepository;
		this.scenarioActRepository = scenarioActRepository;
		this.scenarioActEdgeRepository = scenarioActEdgeRepository;
		this.scenarioActTaskRepository = scenarioActTaskRepository;
		this.scenarioActTaskEdgeRepository = scenarioActTaskEdgeRepository;
		this.taskExecutionServiceGroupRepository = taskExecutionServiceGroupRepository;
        this.scenarioActTemplateService = scenarioActTemplateService;
        this.scheduleConfigProducerService = scheduleConfigProducerService;
        this.scenarioActTaskExecutionModuleArgRepository = scenarioActTaskExecutionModuleArgRepository;

        this.mapper = mapper;
	}

	private ScheduleScenarioActDTO mapScheduleScenarioActToDTO(ScenarioAct scenarioAct) {
		logger.info("mapScheduleScenarioActToDTO: {}", scenarioAct.getName());
		ScheduleScenarioActDTO result = new ScheduleScenarioActDTO();
		result.setName(scenarioAct.getName());
		result.setDataSet(scenarioAct.getDataSet().getName());
		if (scenarioAct.getScenarioActTemplate()!=null)
			result.setScenarioActTemplate(scenarioAct.getScenarioActTemplate().getName());
	    result.setTasks(scenarioActTaskRepository
	    		.findByScenarioActId(scenarioAct.getId())
	    		.stream()
	    		.map(sat-> mapper
						.mapTaskToDTO(sat, getScenarioActTaskExecutionModuleArgsByScenarioActTask(sat.getId())))
				.toList()
	    );
	    result.setDagEdges(
		    scenarioActTaskEdgeRepository
		    	.findByScenarioActId(scenarioAct.getId())
		    	.stream()
		    	.map(sate -> {
		    		DagEdgeDTO dagEdgeDTO = new DagEdgeDTO();
		    		dagEdgeDTO.setFrom(sate.getFromScenarioActTask());
		    		dagEdgeDTO.setTo(sate.getToScenarioActTask());
		    		
		    		return dagEdgeDTO;
		    	})
		    	.toList()
		    );
		return result;
	}

	private ScenarioAct mapScheduleScenarioActToEntity(Schedule schedule,
			ScheduleScenarioActDTO scheduleScenarioActDTO) {

		ScenarioAct result = new ScenarioAct();
		result.setName(scheduleScenarioActDTO.getName());
		result.setSchedule(schedule);
		result.setDataSet(
				dataSetRepository.findById(scheduleScenarioActDTO.getDataSet()).orElseThrow(() -> new RuntimeException(
						String.format("Data set name %s not found", scheduleScenarioActDTO.getDataSet()))));

		if (scheduleScenarioActDTO.getScenarioActTemplate() != null)
			result.setScenarioActTemplate(
				scenarioActTemplateRepository.findById(scheduleScenarioActDTO.getScenarioActTemplate())
						.orElseThrow(() -> new RuntimeException(String.format("Scenario template name %s not found",
								scheduleScenarioActDTO.getScenarioActTemplate()))));

		return result;
	}

	private DagEdgeDTO mapScenarioActEdgesToDTO(ScenarioActEdge scenarioActEdge) {
		DagEdgeDTO result = new DagEdgeDTO();
		result.setFrom(scenarioActEdge.getFromScenarioAct().getName());
		result.setTo(scenarioActEdge.getToScenarioAct().getName());
		return result;
	}

	private ScheduleDTO mapScheduleToDTO(Schedule schedule) {
		ScheduleDTO result = new ScheduleDTO();
		result.setName(schedule.getName());
		result.setDescription(schedule.getDescription());
		result.setIntervalExpression(schedule.getIntervalExpression());
		List<ScenarioAct> s =  scenarioActRepository.findByScheduleName(schedule.getName());
		result.setStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(schedule.getStartDateTime()));
		result.setEnabled(schedule.isEnabled());
		result.setScenarioActs(scenarioActRepository.findByScheduleName(schedule.getName()).stream()
				.map(this::mapScheduleScenarioActToDTO).toList());
		result.setScenarioActEdges(scenarioActEdgeRepository.findByScheduleName(schedule.getName()).stream()
				.map(this::mapScenarioActEdgesToDTO).toList());
		return result;

	}

	private Schedule mapScheduleToEntity(Schedule schedule, ScheduleDTO scheduleDTO) {
		schedule.setName(scheduleDTO.getName());
		schedule.setDescription(scheduleDTO.getDescription());
		schedule.setIntervalExpression(scheduleDTO.getIntervalExpression());
		schedule.setStartDateTime(DateTimeUtils.parceDateTimeFormatWithTZ(scheduleDTO.getStartDateTime()));
		schedule.setEnabled(scheduleDTO.isEnabled());
		schedule.setLastChangedDateTime(DateTimeUtils.now());
		schedule.setLastChangeNumber(schedule.getLastChangeNumber() +1);
		return schedule;
	}
//todo mb move to factory?
	private ScenarioActEdge mapScheduleScenarioActEdgeToEntity(Schedule schedule, DagEdgeDTO dagEdgeDTO) {
		ScenarioActEdge result = new ScenarioActEdge();
		result.setSchedule(schedule);
		scenarioActRepository.findByScheduleNameAndActName(schedule.getName(), dagEdgeDTO.getFrom())
				.ifPresent(result::setFromScenarioAct);
		scenarioActRepository.findByScheduleNameAndActName(schedule.getName(), dagEdgeDTO.getTo())
				.ifPresent(result::setToScenarioAct);
		return result;

	}

	public List<ScheduleDTO> findAll() {
		return scheduleRepository.findAll().stream().map(this::mapScheduleToDTO).toList();
	}

	@Transactional
	public ScheduleDTO save(ScheduleDTO scheduleDTO) {

		Schedule currentScheduleVersion =
				scheduleRepository
						.findById(scheduleDTO.getName())
						.orElse(new Schedule());

		if (scheduleDTO.equals(mapScheduleToDTO(currentScheduleVersion))) {
			logger.info("Schedule configs equals");
			return scheduleDTO;
		}

		Schedule schedule = scheduleRepository
				.save(
						mapScheduleToEntity(
								currentScheduleVersion,
								scheduleDTO));

		scenarioActRepository.deleteByScheduleName(schedule.getName());

		Map<String, ScenarioAct> scenarioActMap = new HashMap<String, ScenarioAct>();

		scenarioActRepository.saveAll(scheduleDTO.getScenarioActs().stream()
				.map(scheduleScenarioActDTO -> mapScheduleScenarioActToEntity(schedule, scheduleScenarioActDTO))
				.toList()).forEach(sa -> scenarioActMap.put(sa.getName(), sa));

		scenarioActEdgeRepository.deleteByScheduleName(schedule.getName());

		scheduleDTO.getScenarioActEdges().stream()
				.map(dagEdgeDTO -> mapScheduleScenarioActEdgeToEntity(schedule, dagEdgeDTO))
				.forEach(scenarioActEdgeRepository::save);

		// --------------------------

		scheduleDTO.getScenarioActs().stream().forEach(saDto -> {

			ScenarioAct scenarioAct = scenarioActMap.get(saDto.getName());
			Map<String, ScenarioActTask> scenarioActTaskMap = new HashMap<String, ScenarioActTask>();

			saDto.getTasks().stream().forEach(taskDTO -> {

				ScenarioActTask task = new ScenarioActTask();
				task.setScenarioAct(scenarioAct);
				task.setName(taskDTO.getName());
				task.setImportance(taskDTO.getImportance());
				task.setExecutionModule(taskDTO.getExecutionModule());
				task.setTaskExecutionServiceGroup(taskExecutionServiceGroupRepository
						.getReferenceById(taskDTO.getTaskExecutionServiceGroupName()));

				task.setDescription(taskDTO.getDescription());

				ScenarioActTask resultTask = scenarioActTaskRepository.save(task);

				scenarioActTaskMap.put(resultTask.getName(), resultTask);

				taskDTO.getExecutionModuleArgs().forEach((k, v) -> {
					ScenarioActTaskExecutionModuleArg executionModuleArg = new ScenarioActTaskExecutionModuleArg();
					executionModuleArg.setScenarioActTask(resultTask);
					executionModuleArg.setKey(k);
					executionModuleArg.setValue(v);
					scenarioActTaskExecutionModuleArgRepository.save(executionModuleArg);
				});

			});

			saDto.getDagEdges().forEach(dagEdgeDTO -> {
				ScenarioActTaskEdge scenarioActTaskEdge = new ScenarioActTaskEdge();
				scenarioActTaskEdge.setScenarioAct(scenarioAct);
				scenarioActTaskEdge.setFromScenarioActTask(dagEdgeDTO.getFrom());
				scenarioActTaskEdge.setToScenarioActTask(dagEdgeDTO.getTo());
				scenarioActTaskEdgeRepository.save(scenarioActTaskEdge);
			});
		});
		// -------------------------
		ScheduleDTO result = mapScheduleToDTO(schedule);
		scheduleConfigProducerService.send(findEffectiveScheduleDTOById(result.getName()) );
		return result;
	}

	public ScheduleDTO findDtoById(String name) {
		return mapScheduleToDTO(findById(name));
	}
	public Schedule findById(String name) {
		return scheduleRepository.findById(name).orElseThrow(() -> {
			logger.info("Can't get name: {}", name);
			return new ScheduleNotFoundException(name);
		});
	}

	@Transactional
	public void deleteById(String name) {
		scheduleRepository.deleteById(name);
	}


	public ScheduleEffectiveDTO findEffectiveScheduleDTOById(String name) {
		try {

			return mapScheduleDTOAndResolveTemplate(
					this.findDtoById(name),
					scenarioActTemplateService.findAllAsMap());
		}catch (Exception  e){
			throw new RuntimeException(e);
		}
	}


	private Map<String,String> getScenarioActTaskExecutionModuleArgsByScenarioActTask(Long scenarioActTaskId){
		return scenarioActTaskExecutionModuleArgRepository
				.findByScenarioActTaskId(scenarioActTaskId)
				.stream()
				.collect(
						Collectors
								.toMap(
										ScenarioActTaskExecutionModuleArg::getKey,
										ScenarioActTaskExecutionModuleArg::getValue));

	}

	private TaskDTO matchTaskWithTemplate (
			TaskDTO taskDTO,
			TaskDTO taskTemplate) {

		if (taskDTO == null && taskTemplate == null)
			logger.warn("Internal error both arguments are null");

		if (taskTemplate == null)
			return taskDTO;

		if(taskDTO == null)
		   return taskTemplate;

		TaskDTO result = new TaskDTO();
		result.setName(Coalesce.apply(taskDTO.getName(), taskTemplate.getName()));
		result.setDescription(Coalesce.apply(taskDTO.getDescription(), taskTemplate.getDescription()));
		result.setImportance(Coalesce.apply(taskDTO.getImportance() , taskTemplate.getImportance()));
		result.setExecutionModuleArgs(Coalesce.applyStringMap(taskDTO.getExecutionModuleArgs() , taskTemplate.getExecutionModuleArgs()));
		result.setExecutionModule(Coalesce.apply(taskDTO.getExecutionModule(),taskTemplate.getExecutionModule()));
		result.setTaskExecutionServiceGroupName(Coalesce.apply(taskDTO.getTaskExecutionServiceGroupName(), taskTemplate.getTaskExecutionServiceGroupName()));
		return result;
	}


	public TaskDTO getEffectiveTaskDTO(String scheduleName, String scenarioActName, String taskName)  {

		ScenarioAct scenarioAct = scenarioActRepository.findByScheduleNameAndActName(scheduleName,scenarioActName)
				.orElseThrow(() -> new ScenarioActNotFoundException(scheduleName,scenarioActName));

		Optional<ScenarioActTask> scenarioActTask = scenarioActTaskRepository
					.findByScenarioActIdAndName(scenarioAct.getId(), taskName);


		TaskDTO taskDTO = null;

		if(scenarioActTask.isPresent())
        	taskDTO = mapper
            	.mapTaskToDTO(
                    scenarioActTask.get(),
                    getScenarioActTaskExecutionModuleArgsByScenarioActTask(scenarioActTask.get().getId()));

		TaskDTO  taskTemplate = null;
		if (scenarioAct.getScenarioActTemplate() != null)
			taskTemplate = scenarioActTemplateService
				.findTaskByScenarioActTemplateAndTaskName(
						scenarioAct.getScenarioActTemplate().getName(),
						taskName);

		TaskDTO result = matchTaskWithTemplate(taskDTO, taskTemplate);
		if (result ==null)
		   throw new TaskEffectiveNotFoundException(scheduleName, scenarioActName, taskName);
		return result;
	}

	public List<ScheduleEffectiveDTO> findScheduleEffectiveDTOSByChangeDateTime(OffsetDateTime dateTime){
		Map<String,ScenarioActTemplateDTO> actTemplateMap = scenarioActTemplateService.findAllAsMap();
		return  scheduleRepository
				.findByLastChangedDateTimeGreaterThan(dateTime)
				.stream()
				.map(s -> {
                            try {
                                return mapScheduleDTOAndResolveTemplate(
                                                  mapScheduleToDTO(s),
                                                    actTemplateMap);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
				).toList();
	}
	private ScheduleEffectiveDTO mapScheduleDTOAndResolveTemplate(
			ScheduleDTO scheduleDTO,
			Map<String,ScenarioActTemplateDTO> actTemplateDTOMap
			) throws Exception {
		ScheduleEffectiveDTO result = new ScheduleEffectiveDTO();
		Schedule schedule = findById(scheduleDTO.getName());
		result.setEnabled(scheduleDTO.isEnabled());
		result.setName(scheduleDTO.getName());
		result.setIntervalExpression(scheduleDTO.getIntervalExpression());
		result.setStartDateTime(scheduleDTO.getStartDateTime());
		result.setStopDateTime(scheduleDTO.getStopDateTime());
		result.setScenarioActEdges(scheduleDTO.getScenarioActEdges());
		result.setDescription(scheduleDTO.getDescription());
		result.setLastChangedDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(schedule.getLastChangedDateTime()));
		result.setLastChangeNumber(schedule.getLastChangeNumber());
		result.setScenarioActs(
			scheduleDTO.getScenarioActs().stream().map(sa-> {

				ScheduleScenarioActEffectiveDTO resultAct = new ScheduleScenarioActEffectiveDTO();
				resultAct.setName(sa.getName());
				resultAct.setDataSet(sa.getDataSet());
				// edges
				Set<DagEdgeDTO> edgeDTOSet = new HashSet<>(sa.getDagEdges());
				edgeDTOSet.addAll(
						scenarioActTemplateService
								.getDagEdgeDTOListNullSafe(
										actTemplateDTOMap.get(sa.getScenarioActTemplate())));
				resultAct.setDagEdges(edgeDTOSet.stream().toList());

				//vertices
				Map<String, TaskDTO> taskDTOmap =
						sa.getTasks()
								.stream().collect(Collectors.toMap(TaskDTO::getName,taskDTO -> taskDTO));

				Map<String,TaskDTO> taskDTOTemplatesMap =
						scenarioActTemplateService
								.getTaskDTOListNullSafe(
										actTemplateDTOMap
												.get(sa.getScenarioActTemplate()))
								.stream()
								.collect(Collectors.toMap(TaskDTO::getName,taskDTO -> taskDTO));

				Set<String> taskNames = new HashSet<>();
				taskNames.addAll(taskDTOmap.keySet());
				taskNames.addAll(taskDTOTemplatesMap.keySet());


				resultAct.setTasks(taskNames
						.stream()
						.map(string -> matchTaskWithTemplate(taskDTOmap.get(string), taskDTOTemplatesMap.get(string)))
						.toList());

				return resultAct;
			}).toList()
		);
		return result;
	}
}
