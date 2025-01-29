package org.lakehouse.scheduler.service;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstance;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstanceDependency;
import org.lakehouse.scheduler.repository.ScheduleInstanceRepository;
import org.lakehouse.scheduler.repository.ScheduleInstanceRunningRepository;
import org.lakehouse.scheduler.repository.ScheduleScenarioActInstanceDependencyRepository;
import org.lakehouse.scheduler.repository.ScheduleScenarioActInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScheduleScenarioActInstanceManageStateService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository;
	private final ScheduleScenarioActInstanceDependencyRepository scenarioActInstanceDependencyRepository;
	public ScheduleScenarioActInstanceManageStateService(
			ScheduleInstanceRunningRepository scheduleInstanceRunnigService,
			ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository,
			ScheduleInstanceRepository scheduleInstanceRepository, 
			ScheduleScenarioActInstanceDependencyRepository scenarioActInstanceDependencyRepository) {
		this.scheduleScenarioActInstanceRepository = scheduleScenarioActInstanceRepository;
		this.scenarioActInstanceDependencyRepository = scenarioActInstanceDependencyRepository;
	}


	public int runNewScenariosActs() {
		List<ScheduleScenarioActInstance> l =
		scheduleScenarioActInstanceRepository
			.findScenarioActReadyToRun();
		l.forEach(ssai -> {
				ssai.setStatus(Status.ScenarioAct.RUNNING.label);
				scheduleScenarioActInstanceRepository.save(ssai);
			});
		return l.size();
	}
	
	public int runSucsessScenariosActs() {
		List<ScheduleScenarioActInstance> l =
		scheduleScenarioActInstanceRepository
			.findScenarioActReadyToSuccess();
		
		l.forEach(ssai -> {
				ssai.setStatus(Status.ScenarioAct.SUCCESS.label);
				scheduleScenarioActInstanceRepository.save(ssai);
			});
		
		l.forEach(s -> satisfyDependencies(
				scenarioActInstanceDependencyRepository
					.findByFrom(s)));
		
		return l.size();
		
		
	}
	@Transactional
	private void satisfyDependencies(List<ScheduleScenarioActInstanceDependency> l) {
		List<ScheduleScenarioActInstanceDependency> deps = 
				l.stream()
				 .map(d -> {
						d.setSatisfied(true);
						return d;
					})
				 .toList();
		scenarioActInstanceDependencyRepository.saveAll(deps);
	}
}
