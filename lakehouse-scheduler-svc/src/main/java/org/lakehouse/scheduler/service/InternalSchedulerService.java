package org.lakehouse.scheduler.service;


import org.lakehouse.cli.api.dto.configs.ScheduleDTO;
import org.lakehouse.cli.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.cli.api.utils.DateTimeUtils;
import org.lakehouse.config.rest.client.service.ClientApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@Profile("prod")
public class InternalSchedulerService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final ScheduleInstanceBuildService scheduleInstanceService;
	private final ScheduleInstanceLastBuildService scheduleInstanceLastBuildService;
	private final ScheduleInstanceRunnigService scheduleInstanceRunnigService;
	private final ScheduleTaskInstanceService scheduleTaskInstanceService;
	private final ScheduleScenarioActInstanceManageStateService scheduleScenarioActInstanceManageStateService;
	private final ClientApi clientApi;
	public InternalSchedulerService(ScheduleInstanceBuildService scheduleInstanceService,
                                    ScheduleInstanceLastBuildService scheduleInstanceLastBuildService,
                                    ScheduleInstanceRunnigService scheduleInstanceRunnigService,
                                    ScheduleTaskInstanceService scheduleTaskInstanceService,
                                    ScheduleScenarioActInstanceManageStateService scheduleScenarioActInstanceManageStateService,
									ClientApi clientApi) {

		this.scheduleInstanceService = scheduleInstanceService;
		this.scheduleInstanceLastBuildService = scheduleInstanceLastBuildService;
		this.scheduleInstanceRunnigService = scheduleInstanceRunnigService;
		this.scheduleTaskInstanceService = scheduleTaskInstanceService;
		this.scheduleScenarioActInstanceManageStateService = scheduleScenarioActInstanceManageStateService;

        this.clientApi = clientApi;
    }
/**
 * Make schedule objects with status  NEW
 * */
	@Scheduled(
			fixedDelayString = "${lakehouse.api.schedule.registration.delay-ms}",
			initialDelayString = "${lakehouse.api.schedule.registration.initial-delay-ms}")
	public void findAndRegisterNewSchedules() {
		DateTimeUtils.now().minus()
		List<ScheduleEffectiveDTO> scheduleDTOs = clientApi.getScheduleEffectiveDTOList();

		scheduleInstanceLastBuildService.findAndRegisterNewSchedules(scheduleDTOs);
		scheduleInstanceRunnigService.findAndRegisterNewSchedules();
		logger.info("findAndRegisterNewSchedules");
		scheduleInstanceService.buildNewSchedules(scheduleDTOs);
		logger.info("buildNewTasks");
	}

	/**
	 * Make schedule objects with status  NEW
	 * */
	@Scheduled(
			fixedDelayString = "${lakehouse.api.schedule.run.delay-ms}", 
			initialDelayString = "${lakehouse.api.schedule.run.initial-delay-ms}")
	public void runSchedules() {
		int rows;
		rows = scheduleInstanceRunnigService.sucsessSchedules();
		logger.info("sucsessSchedules {}", rows );
		
		scheduleInstanceRunnigService.runSchedules();
		logger.info("runSchedules");
		
		rows = scheduleScenarioActInstanceManageStateService.runNewScenariosActs();
		logger.info("runNewScenariosActs {}", rows );
	   
		rows = scheduleTaskInstanceService.addTaskToQueue();
		logger.info("queueTasks {}", rows );
	}


	
	@Scheduled(
			fixedDelayString = "${lakehouse.api.schedule.resolvedeps.delay-ms}", 
			initialDelayString = "${lakehouse.api.schedule.resolvedeps.initial-delay-ms}")
	public void resolveDependency() {
		int rows = scheduleScenarioActInstanceManageStateService.runSucsessScenariosActs();
		logger.info("resolveScenarioActDependency {}", rows);
		
		rows = scheduleTaskInstanceService.successResolvedDependency();
		logger.info("resolveTaskDependency {}", rows);
	}
	

	
	@Scheduled(
			fixedDelayString = "${lakehouse.api.schedule.task.retry.delay-ms}", 
			initialDelayString = "${lakehouse.api.schedule.task.retry.initial-delay-ms}")
	public void reTryFilTasked() {
		int rows;
		
		rows = scheduleTaskInstanceService.heartBeatLimitExceeded();
		logger.info("heartBeatLimitExceeded {}", rows);
		
		rows = scheduleTaskInstanceService.reTryFailedTasks();
		logger.info("reTryFailedTasks {}", rows);
	}
}
