package lakehouse.api.service.tasks;

import lakehouse.api.constant.Status;
import lakehouse.api.entities.configs.ScenarioAct;
import lakehouse.api.entities.configs.Schedule;
import lakehouse.api.entities.configs.TaskTemplate;
import lakehouse.api.entities.tasks.*;
import lakehouse.api.exception.CronParceErrorException;
import lakehouse.api.exception.TransactionException;
import lakehouse.api.mapper.Mapper;
import lakehouse.api.repository.configs.ScenarioActEdgeRepository;
import lakehouse.api.repository.configs.ScenarioActRepository;
import lakehouse.api.repository.configs.TaskTemplateEdgeRepository;
import lakehouse.api.repository.configs.TaskTemplateRepository;
import lakehouse.api.repository.tasks.*;
import lakehouse.api.utils.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleInstanceService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final ScheduleInstanceLastRepository scheduleInstanceLastRepository;
	private final ScheduleInstanceRepository scheduleInstanceRepository;
	private final ScheduleTaskInstanceRepository scheduleTaskInstanceRepository;
	private final TaskTemplateRepository taskTemplateRepository;
	private final TaskTemplateEdgeRepository taskTemplateEdgeRepository;
	private final ScheduleTaskInstanceDependencyRepository scheduleTaskInstanceDependencyRepository;
	private final ScenarioActRepository scenarioActRepository;
	private final ScenarioActEdgeRepository scenarioActEdgeRepository;
	private final Mapper mapper;
	private final ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository;
	private final ScheduleScenarioActInstanceDependencyRepository scheduleScenarioActInstanceDependencyRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	public ScheduleInstanceService(ScheduleInstanceLastRepository scheduleInstanceLastRepository,
			ScheduleInstanceRepository scheduledTaskRepository,
			ScheduleTaskInstanceRepository scheduleTaskInstanceRepository,
			TaskTemplateRepository taskTemplateRepository, TaskTemplateEdgeRepository taskTemplateEdgeRepository,
			ScheduleTaskInstanceDependencyRepository scheduleTaskInstanceDependencyRepository,
			ScenarioActRepository scenarioActRepository, ScenarioActEdgeRepository scenarioActEdgeRepository,
			Mapper mapper, ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository,
			ScheduleScenarioActInstanceDependencyRepository scheduleScenarioActInstanceDependencyRepository) {
		this.scheduleInstanceLastRepository = scheduleInstanceLastRepository;
		this.scheduleInstanceRepository = scheduledTaskRepository;
		this.scheduleTaskInstanceRepository = scheduleTaskInstanceRepository;
		this.taskTemplateRepository = taskTemplateRepository;
		this.taskTemplateEdgeRepository = taskTemplateEdgeRepository;
		this.scheduleTaskInstanceDependencyRepository = scheduleTaskInstanceDependencyRepository;
		this.scenarioActRepository = scenarioActRepository;
		this.scenarioActEdgeRepository = scenarioActEdgeRepository;
		this.mapper = mapper;
		this.scheduleScenarioActInstanceRepository = scheduleScenarioActInstanceRepository;
		this.scheduleScenarioActInstanceDependencyRepository = scheduleScenarioActInstanceDependencyRepository;
	}

	private ScheduleInstance newScheduleInstance(ScheduleInstanceLast scheduleInstanceLast) {
		ScheduleInstance scheduleInstance = new ScheduleInstance();
		scheduleInstance.setSchedule(scheduleInstanceLast.getSchedule());
		OffsetDateTime lastTargetExecutionDate;

		if (scheduleInstanceLast.getScheduleInstance() == null) {
			lastTargetExecutionDate = scheduleInstanceLast.getSchedule().getStartDateTime();
		} else {
			lastTargetExecutionDate = scheduleInstanceLast.getScheduleInstance().getTargetExecutionDateTime();
		}

		try {
			scheduleInstance.setTargetExecutionDateTime(DateTimeUtils.getNextTargetExecutionDateTime(
					scheduleInstanceLast.getSchedule().getIntervalExpression(), lastTargetExecutionDate));
		} catch (CronParceErrorException e) {
			logger.warn(e.getMessage());
			throw new TransactionException(String.format("Error when try to set TargetExecutionDateTime of %s",
					scheduleInstance.getSchedule().getName()), e);
		}

		scheduleInstance.setStatus(Status.Schedule.NEW.label);

		return scheduleInstance;
	}

	private List<ScheduleScenarioActInstance> getScheduleScenarioActInstances(ScheduleInstance scheduleInstance,
			List<ScenarioAct> scenarioActs) {
		return scenarioActs.stream()
				.map(scenarioAct -> mapper.mapToScheduleScenarioActInstance(scenarioAct, scheduleInstance)).toList();
	}

	private List<ScheduleTaskInstance> getTaskInstancesByAct(Map<String, ScheduleScenarioActInstance> actInstanceMap,
			Map<ScenarioAct, Map<String, TaskTemplate>> scenarioTaskTemplateMap

	) {
		return scenarioTaskTemplateMap.keySet().stream().flatMap(
				scenarioAct -> scenarioTaskTemplateMap.get(scenarioAct).values().stream().map(taskTemplate -> mapper
						.mapToScheduleTaskInstance(taskTemplate, actInstanceMap.get(scenarioAct.getName()))))
				.toList();
	}

	private List<ScheduleScenarioActInstanceDependency> getScheduleScenarioActInstanceDependencies(
			ScheduleInstance scheduleInstance, Map<String, ScheduleScenarioActInstance> actInstanceMap) {

		return scenarioActEdgeRepository.findByScheduleName(scheduleInstance.getSchedule().getName()).stream()
				.map(scenarioActEdge -> mapper.mapToScheduleScenarioActInstanceDependency(
						actInstanceMap.get(scenarioActEdge.getFromScenarioAct().getName()),
						actInstanceMap.get(scenarioActEdge.getToScenarioAct().getName())))
				.toList();

	}

	private List<ScheduleTaskInstanceDependency> getScheduleTaskInstanceDependencies(List<ScenarioAct> scenarioActs,
			List<ScheduleTaskInstance> scheduleTaskInstances) {

		Map<ScenarioAct, Map<String, ScheduleTaskInstance>> taskInstanceMapByAct = new HashMap<>();

		scenarioActs.forEach(scenarioAct -> {

			Map<String, ScheduleTaskInstance> taskInstanceMap = new HashMap<>();

			scheduleTaskInstances.stream()
					.filter(sti -> sti.getScheduleScenarioActInstance().getName().equals(scenarioAct.getName()))
					.forEach(sti -> taskInstanceMap.put(sti.getName(), sti));

			taskInstanceMapByAct.put(scenarioAct, taskInstanceMap);

		});

		List<ScheduleTaskInstanceDependency> scheduleTaskInstanceDependencies = taskInstanceMapByAct.keySet().stream()
				.flatMap(sa -> taskTemplateEdgeRepository
						.findByScenarioTemplateName(sa.getScenarioActTemplate().getName()).stream()
						.map(taskTemplateEdge -> mapper.mapToScheduleTaskInstanceDependency(
								taskInstanceMapByAct.get(sa).get(taskTemplateEdge.getFromTaskTemplate().getName()),
								taskInstanceMapByAct.get(sa).get(taskTemplateEdge.getToTaskTemplate().getName()))))
				.toList();

		return scheduleTaskInstanceDependencies;
	}

	private Map<ScenarioAct, Map<String, TaskTemplate>> getScenarioTaskTemplateMap(List<ScenarioAct> scenarioActs) {
		Map<ScenarioAct, Map<String, TaskTemplate>> result = new HashMap<>();
		scenarioActs.forEach(scenarioAct -> {

			Map<String, TaskTemplate> taskTemplateMap = new HashMap<>();

			taskTemplateRepository.findByScenarioTemplateName(scenarioAct.getScenarioActTemplate().getName())
					.forEach(tt -> taskTemplateMap.put(tt.getName(), tt));
			result.put(scenarioAct, taskTemplateMap);
		});
		return result;
	}

	private void save(ScheduleInstanceLast scheduleInstanceLast) {
		Schedule schedule = scheduleInstanceLast.getSchedule();
		List<ScenarioAct> scenarioActs = scenarioActRepository.findByScheduleName(schedule.getName());

		Map<ScenarioAct, Map<String, TaskTemplate>> scenarioTaskTemplateMap = getScenarioTaskTemplateMap(scenarioActs);

		transactionTemplate.execute(status -> {
			try {

				ScheduleInstance scheduleInstance = scheduleInstanceRepository
						.save(newScheduleInstance(scheduleInstanceLast));

				List<ScheduleScenarioActInstance> scheduleScenarioActInstances = scheduleScenarioActInstanceRepository
						.saveAll(getScheduleScenarioActInstances(scheduleInstance, scenarioActs));

				Map<String, ScheduleScenarioActInstance> actInstanceMap = new HashMap<>();
				scheduleScenarioActInstances.forEach(ssai -> actInstanceMap.put(ssai.getName(), ssai));

				scheduleScenarioActInstanceDependencyRepository
						.saveAll(getScheduleScenarioActInstanceDependencies(scheduleInstance, actInstanceMap));

				List<ScheduleTaskInstance> scheduleTaskInstances = scheduleTaskInstanceRepository
						.saveAll(getTaskInstancesByAct(actInstanceMap, scenarioTaskTemplateMap));

				scheduleTaskInstanceDependencyRepository
						.saveAll(getScheduleTaskInstanceDependencies(scenarioActs, scheduleTaskInstances));

				scheduleInstanceLast.setScheduleInstance(scheduleInstance);
				scheduleInstanceLastRepository.save(scheduleInstanceLast);
			} catch (Exception e) {

				logger.warn("err", e);
				throw e;
			}
			return status;
		});
	}

	public void buildNewSchedules() {
		scheduleInstanceLastRepository.findByScheduleEnabled().forEach(scheduleInstanceLast -> {
			try {
				save(scheduleInstanceLast);
			} catch (Exception e) {
				logger.warn(e.getMessage());
				throw new RuntimeException(e);
			}
		});
	}
}
