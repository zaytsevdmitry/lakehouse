package org.lakehouse.scheduler.service;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.configs.ScheduleScenarioActEffectiveDTO;
import org.lakehouse.client.api.dto.configs.TaskDTO;
import org.lakehouse.client.api.exception.CronParceErrorException;
import org.lakehouse.client.api.utils.DateTimeUtils;

import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.scheduler.entities.ScheduleInstance;
import org.lakehouse.scheduler.entities.ScheduleInstanceLastBuild;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstance;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstanceDependency;
import org.lakehouse.scheduler.entities.ScheduleTaskInstance;
import org.lakehouse.scheduler.entities.ScheduleTaskInstanceDependency;
import org.lakehouse.scheduler.exception.TransactionException;
import org.lakehouse.scheduler.repository.ScheduleInstanceLastBuildRepository;
import org.lakehouse.scheduler.repository.ScheduleInstanceRepository;
import org.lakehouse.scheduler.repository.ScheduleScenarioActInstanceDependencyRepository;
import org.lakehouse.scheduler.repository.ScheduleScenarioActInstanceRepository;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceDependencyRepository;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScheduleInstanceBuildService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final ScheduleInstanceLastBuildRepository scheduleInstanceLastBuildRepository;
	private final ScheduleInstanceRepository scheduleInstanceRepository;
	private final ScheduleTaskInstanceRepository scheduleTaskInstanceRepository;

	private final ScheduleTaskInstanceDependencyRepository scheduleTaskInstanceDependencyRepository;
	private final ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository;
	private final ScheduleScenarioActInstanceDependencyRepository scheduleScenarioActInstanceDependencyRepository;
	private final ConfigRestClientApi configRestClientApi;
	@Autowired
	public ScheduleInstanceBuildService(
            ScheduleInstanceLastBuildRepository scheduleInstanceLastBuildRepository,
            ScheduleInstanceRepository scheduledTaskRepository,
            ScheduleTaskInstanceRepository scheduleTaskInstanceRepository,
            ScheduleTaskInstanceDependencyRepository scheduleTaskInstanceDependencyRepository,
            ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository,
            ScheduleScenarioActInstanceDependencyRepository scheduleScenarioActInstanceDependencyRepository,
			ConfigRestClientApi configRestClientApi) {
		this.scheduleInstanceLastBuildRepository = scheduleInstanceLastBuildRepository;
		this.scheduleInstanceRepository = scheduledTaskRepository;
		this.scheduleTaskInstanceRepository = scheduleTaskInstanceRepository;

		this.scheduleTaskInstanceDependencyRepository = scheduleTaskInstanceDependencyRepository;

		this.scheduleScenarioActInstanceRepository = scheduleScenarioActInstanceRepository;
		this.scheduleScenarioActInstanceDependencyRepository = scheduleScenarioActInstanceDependencyRepository;
        this.configRestClientApi = configRestClientApi;
    }

	private ScheduleInstance newScheduleInstance(
			ScheduleInstanceLastBuild scheduleInstanceLast,
			ScheduleEffectiveDTO scheduleEffectiveDTO) {
		ScheduleInstance scheduleInstance = new ScheduleInstance();
		scheduleInstance.setConfigScheduleKeyName(scheduleInstanceLast.getConfigScheduleKeyName());
		OffsetDateTime lastTargetExecutionDate;

		if (scheduleInstanceLast.getScheduleInstance() == null) {
			lastTargetExecutionDate =  DateTimeUtils.parceDateTimeFormatWithTZ( scheduleEffectiveDTO.getStartDateTime()); //scheduleInstanceLast.getSchedule().getStartDateTime();
		} else {
			lastTargetExecutionDate = scheduleInstanceLast.getScheduleInstance().getTargetExecutionDateTime();
		}

		try {
			scheduleInstance.setTargetExecutionDateTime(DateTimeUtils.getNextTargetExecutionDateTime(
					scheduleEffectiveDTO.getIntervalExpression(), lastTargetExecutionDate));
		} catch (CronParceErrorException e) {
			logger.warn(e.getMessage());
			throw new TransactionException(String.format("Error when try to set TargetExecutionDateTime of %s",
					scheduleEffectiveDTO.getName()), e);
		}

		scheduleInstance.setStatus(Status.Schedule.NEW.label);

		return scheduleInstance;
	}

	public ScheduleScenarioActInstance mapToScheduleScenarioActInstance(
			ScheduleScenarioActEffectiveDTO scheduleScenarioActEffectiveDTO,
			ScheduleInstance scheduleInstance) {
		ScheduleScenarioActInstance result = new ScheduleScenarioActInstance();
		result.setName(scheduleScenarioActEffectiveDTO.getName());
		result.setScheduleInstance(scheduleInstance);
		result.setConfDataSetKeyName(scheduleScenarioActEffectiveDTO.getDataSet());
		result.setStatus(Status.ScenarioAct.NEW.label);
		return result;
	}

	public ScheduleTaskInstance mapToNewScheduleTaskInstance(
			TaskDTO taskDTO,
			ScheduleScenarioActInstance scheduleScenarioActInstance) {
		ScheduleTaskInstance result = new ScheduleTaskInstance();
		result.setName(taskDTO.getName());
		result.setScheduleScenarioActInstance(scheduleScenarioActInstance);
		result.setStatus(Status.Task.NEW.label);
		return result;
	}
	@Transactional
	private void save(
			ScheduleInstanceLastBuild scheduleInstanceLastBuild,
			ScheduleEffectiveDTO scheduleEffectiveDTO) throws Exception {


		ScheduleInstance scheduleInstance = scheduleInstanceRepository
				.save(newScheduleInstance(scheduleInstanceLastBuild, scheduleEffectiveDTO));

		Map<String,ScheduleScenarioActInstance> actInstanceMap =
				scheduleEffectiveDTO.getScenarioActs().stream().map(scheduleScenarioActEffectiveDTO -> {

				ScheduleScenarioActInstance ssai = scheduleScenarioActInstanceRepository
						.save(
								mapToScheduleScenarioActInstance(
										scheduleScenarioActEffectiveDTO,
										scheduleInstance));


				Map<String,ScheduleTaskInstance> taskInstanceMap =
						scheduleScenarioActEffectiveDTO.getTasks().stream().map( taskDTO -> {
							ScheduleTaskInstance sti =
									scheduleTaskInstanceRepository
											.save(mapToNewScheduleTaskInstance(taskDTO,ssai));

							return sti;
						}).collect(Collectors.toMap(ScheduleTaskInstance::getName, sti -> sti));
				scheduleScenarioActEffectiveDTO.getDagEdges().forEach(dagEdgeDTO -> {
					ScheduleTaskInstanceDependency stid = new ScheduleTaskInstanceDependency();
					stid.setScheduleTaskInstance(taskInstanceMap.get(dagEdgeDTO.getTo()));
					stid.setDepends(taskInstanceMap.get(dagEdgeDTO.getFrom()));
					scheduleTaskInstanceDependencyRepository.save(stid);
				});
			return ssai;
		}).collect(Collectors.toMap(ScheduleScenarioActInstance::getName,ssai -> ssai));

		scheduleEffectiveDTO.getScenarioActEdges().forEach(dagEdgeDTO -> {
			ScheduleScenarioActInstanceDependency ssaid = new ScheduleScenarioActInstanceDependency();
			ssaid.setFrom(actInstanceMap.get(dagEdgeDTO.getFrom()));
			ssaid.setTo(actInstanceMap.get(dagEdgeDTO.getTo()));
			scheduleScenarioActInstanceDependencyRepository.save(ssaid);
		});
	}



	public void buildNewSchedules() {

		scheduleInstanceLastBuildRepository
				.findByEnabled(true)
				.stream()
				.forEach(scheduleInstanceLastBuild -> {
					try {
						save(scheduleInstanceLastBuild,
								configRestClientApi.getScheduleEffectiveDTO(
										scheduleInstanceLastBuild.getConfigScheduleKeyName()));
					} catch (Exception e) {
						logger.warn("Error when build schedule",e);
						throw new RuntimeException(e);
					}
				});
	}
}
